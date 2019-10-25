/**
 * Copyright (C) 2001-2019 by RapidMiner and the contributors
 * 
 * Complete list of developers available at our web site:
 * 
 * http://rapidminer.com
 * 
 * This program is free software: you can redistribute it and/or modify it under the terms of the
 * GNU Affero General Public License as published by the Free Software Foundation, either version 3
 * of the License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Affero General Public License for more details.
 * 
 * You should have received a copy of the GNU Affero General Public License along with this program.
 * If not, see http://www.gnu.org/licenses/.
*/
package base.operators.io.process;

import base.operators.BreakpointListener;
import base.operators.Process;
import base.operators.ProcessContext;
import base.operators.RapidMiner;
import base.operators.operator.*;
import base.operators.operator.ports.InputPort;
import base.operators.operator.ports.OutputPort;
import base.operators.operator.ports.OutputPorts;
import base.operators.parameter.*;
import base.operators.parameter.conditions.*;
import base.operators.tools.container.Pair;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.IOException;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;


/**
 *
 * @author Simon Fischer
 */
public class XMLExporter {

	public static final String ELEMENT_PROCESS = "process";
	private boolean onlyCoreElements = false;

	public XMLExporter() {
		this(false);
	}

	/**
	 *
	 * @param onlyCoreElements
	 *            If true, GUI and other additional information will be ignored.
	 */
	public XMLExporter(boolean onlyCoreElements) {
		this.onlyCoreElements = onlyCoreElements;
	}

	/**
	 * This method will return append the description of this process to the given father element.
	 */
	public void exportProcess(Element fatherElement, Process process) {
		Element rootElement = XMLTools.addTag(fatherElement, ELEMENT_PROCESS);

		rootElement.setAttribute("version", RapidMiner.getLongVersion());

		Document doc = rootElement.getOwnerDocument();

		rootElement.appendChild(exportProcessContext(process.getContext(), doc));
		if (!process.getAnnotations().isEmpty()) {
			rootElement.appendChild(exportAnnotations(process.getAnnotations(), doc));
		}

		rootElement.appendChild(exportOperator(process.getRootOperator(), false, doc));
	}

	/**
	 * This method will create a document, append the complete process that contains the given
	 * operator. The {@link Document} is then returned.
	 */
	public Document exportProcess(Operator operator, boolean hideDefault) throws IOException {
		try {
			Document doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().newDocument();
			Element rootElement = doc.createElement(ELEMENT_PROCESS);
			doc.appendChild(rootElement);
			rootElement.setAttribute("version", RapidMiner.getLongVersion());

			final Process process = operator.getProcess();
			if (process != null) {
				rootElement.appendChild(exportProcessContext(process.getContext(), doc));
				if (!process.getAnnotations().isEmpty()) {
					rootElement.appendChild(exportAnnotations(process.getAnnotations(), doc));
				}
			}
			rootElement.appendChild(exportOperator(operator, hideDefault, doc));
			return doc;
		} catch (ParserConfigurationException e) {
			throw new IOException("Cannot create XML document builder: " + e, e);
		}
	}

	public Document exportSingleOperator(Operator operator) throws IOException {
		try {
			Document doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().newDocument();
			doc.appendChild(exportOperator(operator, false, doc));
			return doc;
		} catch (ParserConfigurationException e) {
			throw new IOException("Cannot create XML document builder: " + e, e);
		}

	}

	private Element exportOperator(Operator operator, boolean hideDefault, Document doc) {
		Element opElement = doc.createElement("operator");
		opElement.setAttribute("name", operator.getName());
		opElement.setAttribute("class", operator.getOperatorDescription().getKey());
		OperatorVersion opVersion = operator.getCompatibilityLevel();
		if (opVersion == null) {
			opVersion = OperatorVersion.getLatestVersion(operator.getOperatorDescription());
		}
		opElement.setAttribute("compatibility", opVersion.toString());

		StringBuilder breakpointString = new StringBuilder();
		boolean first = true;
		for (int i = 0; i < BreakpointListener.BREAKPOINT_POS_NAME.length; i++) {
			if (operator.hasBreakpoint(i)) {
				if (first) {
					first = false;
				} else {
					breakpointString.append(",");
				}
				breakpointString.append(BreakpointListener.BREAKPOINT_POS_NAME[i]);
			}
		}
		if (!first) {
			opElement.setAttribute("breakpoints", breakpointString.toString());
		}

		opElement.setAttribute("expanded", operator.isExpanded() ? "true" : "false");
		opElement.setAttribute("activated", operator.isEnabled() ? "true" : "false");

		operator.getParameters().appendXML(opElement, hideDefault, doc);
		if (operator instanceof OperatorChain) {
			OperatorChain nop = (OperatorChain) operator;
			for (ExecutionUnit executionUnit : nop.getSubprocesses()) {
				opElement.appendChild(exportExecutionUnit(executionUnit, hideDefault, doc, false));
			}
		}
		if (!onlyCoreElements) {
			ProcessXMLFilterRegistry.fireOperatorExported(operator, opElement);
		}
		return opElement;
	}

	private Element exportExecutionUnit(ExecutionUnit executionUnit, boolean hideDefault, Document doc, boolean isRoot) {
		Element procElement;
		if (isRoot) {
			procElement = doc.createElementNS(XMLTools.SCHEMA_URL_PROCESS, ELEMENT_PROCESS);
		} else {
			procElement = doc.createElement(ELEMENT_PROCESS);
		}

		procElement.setAttribute("expanded", executionUnit.isExpanded() ? "true" : "false");

		for (Operator op : executionUnit.getOperators()) {
			procElement.appendChild(exportOperator(op, hideDefault, doc));
		}
		exportConnections(executionUnit.getInnerSources(), executionUnit, procElement, doc);
		for (Operator op : executionUnit.getOperators()) {
			exportConnections(op.getOutputPorts(), executionUnit, procElement, doc);
		}
		if (!onlyCoreElements) {
			ProcessXMLFilterRegistry.fireExecutionUnitExported(executionUnit, procElement);
		}
		return procElement;
	}

	private void exportConnections(OutputPorts outputPorts, ExecutionUnit processInScope, Element insertInto, Document doc) {
		for (OutputPort outputPort : outputPorts.getAllPorts()) {
			if (outputPort.isConnected()) {
				Element portElement = doc.createElement("connect");
				if (processInScope.getEnclosingOperator() != outputPorts.getOwner().getOperator()) {
					portElement.setAttribute("from_op", outputPorts.getOwner().getOperator().getName());
				}
				portElement.setAttribute("from_port", outputPort.getName());
				InputPort destination = outputPort.getDestination();
				if (processInScope.getEnclosingOperator() != destination.getPorts().getOwner().getOperator()) {
					portElement.setAttribute("to_op", destination.getPorts().getOwner().getOperator().getName());
				}
				portElement.setAttribute("to_port", destination.getName());
				insertInto.appendChild(portElement);
			}
		}
	}

	private static void appendList(Element element, String name, List<String> locations) {
		Document doc = element.getOwnerDocument();
		Element list = doc.createElement(name);
		element.appendChild(list);

		// We don't write the last empty entries, so strip first.
		LinkedList<String> nonNull = new LinkedList<>(locations);
		Collections.reverse(nonNull);
		Iterator<String> i = nonNull.iterator();
		while (i.hasNext()) {
			String loc = i.next();
			if (loc != null && !loc.isEmpty()) {
				break;
			}
			i.remove();
		}

		Collections.reverse(nonNull);
		for (String loc : nonNull) {
			Element stringElem = doc.createElement("location");
			list.appendChild(stringElem);
			stringElem.appendChild(doc.createTextNode(loc));
		}
	}

	private Element exportProcessContext(ProcessContext context, Document doc) {
		Element element = doc.createElement("context");
		appendList(element, "input", context.getInputRepositoryLocations());
		appendList(element, "output", context.getOutputRepositoryLocations());
		Element macrosElem = doc.createElement("macros");
		element.appendChild(macrosElem);
		for (Pair<String, String> macro : context.getMacros()) {
			Element macroElement = doc.createElement("macro");
			macrosElem.appendChild(macroElement);
			Element key = doc.createElement("key");
			macroElement.appendChild(key);
			key.appendChild(doc.createTextNode(macro.getFirst()));

			Element value = doc.createElement("value");
			macroElement.appendChild(value);
			value.appendChild(doc.createTextNode(macro.getSecond()));
		}
		return element;
	}

	private Element exportAnnotations(Annotations annotations, Document doc) {
		return annotations.toXML(doc);
	}

	public Document exportParameters(Operator operator, OperatorDescription description) throws ParserConfigurationException {
		Document doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().newDocument();

		Element opElement = doc.createElement("operator");
		doc.appendChild(opElement);
		opElement.setAttribute("class", description.getKey());
		opElement.setAttribute("isHidden", "false");
		opElement.setAttribute("compatibility", "9.3.001");
		Element element = null;
		for(ParameterType type : operator.getParameterTypes()) {

			if(type.isDeprecated()){
				continue;
			}
			if(type.realHidden()){
				continue;
			}
			if (type instanceof ParameterTypeList) {
				element = doc.createElement("list");
				element.setAttribute("key", type.getKey());
				element.setAttribute("type", pType(type));
				element.setAttribute("value", "");
				element.setAttribute("value", "");

				((ParameterTypeList)type).getDefinitionToXML(element, doc);


			} else {
				element = doc.createElement("parameter");

				element.setAttribute("key", type.getKey());
				element.setAttribute("name", "");
				element.setAttribute("type", pType(type));
				element.setAttribute("description", type.getDescription());
			}
			try{
				String def = type.getDefaultValueAsString();
				element.setAttribute("default", def);
			}catch (Exception e){

			}

			if(type.isNumerical()){
				Object a = ((ParameterTypeNumber) type).getMinValue();
				String s = String.valueOf(a);
				if ("-Infinity".equals(s)){
					s = "-1.0 / 0.0";
				}
				element.setAttribute("min", s);
				Object b = ((ParameterTypeNumber) type).getMaxValue();
				s = String.valueOf(b);
				if ("Infinity".equals(s)){
					s = "1.0 / 0.0";
				}
				element.setAttribute("max", s);
			}else if(type instanceof ParameterTypeStringCategory){
				Element valuseElement = doc.createElement("Values");
				String[] values = ((ParameterTypeStringCategory)type).getValues();
				for(String value : values){
					Element valusElement = doc.createElement("Value");
					valusElement.setAttribute("value", value);
					valuseElement.appendChild(valusElement);
				}
				element.appendChild(valuseElement);

			}else if(type instanceof ParameterTypeCategory){
				element.setAttribute("default", String.valueOf(((ParameterTypeCategory) type).getDefault()));
				Element valuseElement = doc.createElement("Values");
				int n = ((ParameterTypeCategory)type).getNumberOfCategories();
				for(int i=0; i<n; ++i){
					Element valueElement = doc.createElement("Value");
					valueElement.setAttribute("zhName", "");
					valueElement.setAttribute("enName", ((ParameterTypeCategory) type).getCategory(i));
					valueElement.setAttribute("value", String.valueOf(i));
					valuseElement.appendChild(valueElement);

				}
				element.appendChild(valuseElement);

			} else if(type instanceof ParameterTypeTupel){
				System.out.println("完善代码:ParameterTypeTupel");
				throw new ParserConfigurationException();
				//Element childTypeselement = doc.createElement("ChildTypes");

			}else if(type instanceof ParameterTypeRegexp){

			}else if(type instanceof ParameterTypeEnumeration){
				Element childTypesElement = XMLTools.addTag(element,"ChildTypes");
				Element childTypeElement = XMLTools.addTag(childTypesElement, "ChildType");
				ParameterType type1 = ((ParameterTypeEnumeration) type).getParameterType();
				childTypeElement.setAttribute("key", type1.getKey());
				childTypeElement.setAttribute("type", "string");
				childTypeElement.setAttribute("value", "");
				childTypeElement.setAttribute("description", type1.getDescription());



			}else if(type instanceof ParameterTypeFile){
				System.out.println("完善代码:ParameterTypeFile");
				continue;
				//throw new ParserConfigurationException();
			}else if(type instanceof ParameterTypeAttribute){

				((ParameterTypeAttribute)type).getDefinitionToXML(element);
				element.setAttribute("portName", ((ParameterTypeAttribute.InputPortMetaDataProvider)((ParameterTypeAttribute)type).getMetaDataProvider()).getInputPort().getName());
			}
			Element conditionElements = null;
			int flag = 0;
			for(ParameterCondition condition : type.getConditions()){
				if(flag == 0){
					conditionElements = doc.createElement("Conditions");
					flag += 1;
				}
				Element conditionElement = doc.createElement("Condition");
				if(type.getKey().equals(condition.getConditionParameter())){
					continue;
				}




				try {
					condition.getDefinitionAsXML(conditionElement);

				} catch (Exception e) {

				}

				setConditionType(condition, conditionElement);

				conditionElements.appendChild(conditionElement);
			}
			if(conditionElements != null){
				element.appendChild(conditionElements);
			}
			opElement.appendChild(element);
		}


		return doc;

	}

	public static void setConditionType(ParameterCondition condition, Element conditionElement){
        conditionElement.setAttribute("key", condition.getConditionParameter());
		if(condition instanceof NonEqualStringCondition){
			conditionElement.setAttribute("type", "nonetypestring");
			conditionElement.setAttribute("description", "NonEqualStringCondition");
		}else if(condition instanceof EqualStringCondition) {
			conditionElement.setAttribute("type", "typestring");
			conditionElement.setAttribute("description", "EqualStringCondition");
		}else if(condition instanceof NonEqualTypeCondition){
			conditionElement.setAttribute("type", "nontype");
			conditionElement.setAttribute("description", "NonEqualTypeCondition");
		}else if(condition instanceof EqualTypeCondition){
			conditionElement.setAttribute("type", "type");
			conditionElement.setAttribute("description", "EqualTypeCondition");
		}else if(condition instanceof AboveOperatorVersionCondition){
			conditionElement.setAttribute("type", "aboveversion");
			conditionElement.setAttribute("description", "AboveOperatorVersionCondition");
		}else if(condition instanceof BelowOrEqualOperatorVersionCondition){
			conditionElement.setAttribute("type", "beloworequalversion");
			conditionElement.setAttribute("description", "BelowOrEqualOperatorVersionCondition");
		}else if(condition instanceof EqualOperatorVersionCondition){
			conditionElement.setAttribute("type", "equalversion");
			conditionElement.setAttribute("description", "EqualOperatorVersionCondition");
		}else if(condition instanceof AndParameterCondition){
			conditionElement.setAttribute("type", "and");
			conditionElement.setAttribute("description", "AndParameterCondition");
		}else if(condition instanceof OrParameterCondition){
			conditionElement.setAttribute("type", "or");
			conditionElement.setAttribute("description", "OrParameterCondition");
		}
	}

	public static void par2XML(Element element, ParameterType type, Document doc)  {


		if (type instanceof ParameterTypeList) {
			element.setAttribute("key", type.getKey());
			element.setAttribute("type", pType(type));
			element.setAttribute("value", "");

			((ParameterTypeList)type).getDefinitionToXML(element, doc);


		} else {

			element.setAttribute("key", type.getKey());
			element.setAttribute("name", "");
			element.setAttribute("type", pType(type));
			element.setAttribute("description", type.getDescription());
		}
		try{
			String def = type.getDefaultValueAsString();
			element.setAttribute("default", def);
		}catch (Exception e){

		}

		if(type.isNumerical()){
			Object a = ((ParameterTypeNumber) type).getMinValue();
			String s = String.valueOf(a);
			if ("-Infinity".equals(s)){
				s = "-1.0 / 0.0";
			}
			element.setAttribute("min", s);
			Object b = ((ParameterTypeNumber) type).getMaxValue();
			s = String.valueOf(b);
			if ("Infinity".equals(s)){
				s = "1.0 / 0.0";
			}
			element.setAttribute("max", s);
		}else if(type instanceof ParameterTypeStringCategory){
			Element valuseElement = doc.createElement("Values");
			String[] values = ((ParameterTypeStringCategory)type).getValues();
			for(String value : values){
				Element valusElement = doc.createElement("Value");
				valusElement.setAttribute("value", value);
				valuseElement.appendChild(valusElement);
			}
			element.appendChild(valuseElement);

		}else if(type instanceof ParameterTypeCategory){
			element.setAttribute("default", String.valueOf(((ParameterTypeCategory) type).getDefault()));
			Element valuseElement = doc.createElement("Values");
			int n = ((ParameterTypeCategory)type).getNumberOfCategories();
			for(int i=0; i<n; ++i){
				Element valueElement = doc.createElement("Value");
				valueElement.setAttribute("zhName", "");
				valueElement.setAttribute("enName", ((ParameterTypeCategory) type).getCategory(i));
				valueElement.setAttribute("value", String.valueOf(i));
				valuseElement.appendChild(valueElement);

			}
			element.appendChild(valuseElement);

		} else if(type instanceof ParameterTypeTupel){
			System.out.println("完善代码:ParameterTypeTupel");
			//Element childTypeselement = doc.createElement("ChildTypes");

		}else if(type instanceof ParameterTypeRegexp){
			System.out.println("完善代码:ParameterTypeRegexp");
		}else if(type instanceof ParameterTypeFile){
			System.out.println("完善代码:ParameterTypeFile");
		}else if(type instanceof ParameterTypeAttribute){

			((ParameterTypeAttribute)type).getDefinitionToXML(element);
			element.setAttribute("portName", ((ParameterTypeAttribute.InputPortMetaDataProvider)((ParameterTypeAttribute)type).getMetaDataProvider()).getInputPort().getName());
		}
	}

	private static String pType(ParameterType type){
		if(type instanceof ParameterTypeRegexp){
			return "regexp";
		}
		String name = type.getClass().getSimpleName();
		if(name.length()<13){
			System.out.println("____________________"+type);
		}
		String temp = name.substring(13).toLowerCase();
		if ("enumeration".equals(temp)){
			return "enum";
		}else {
			return temp;
		}
	}
}
