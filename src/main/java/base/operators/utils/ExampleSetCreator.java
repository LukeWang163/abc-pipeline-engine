package base.operators.utils;

import base.operators.example.Attribute;
import base.operators.example.ExampleSet;
import base.operators.example.table.AttributeFactory;
import base.operators.example.utils.ExampleSetBuilder;
import base.operators.example.utils.ExampleSets;
import base.operators.tools.Ontology;

import java.util.*;

/**
 * @author zls
 * create time:  2019.09.10.
 * description:
 */
public class ExampleSetCreator {


    /**
     * @param map : key列名：value类型
     * @param data ：数据（行），List<Object>为一行
     *             未处理可能异常
     * @return
     */
    public static ExampleSet createExampleSet(Map<String, String> map, List<List<Object> > data) throws Exception {
        List<Attribute> attributes = new ArrayList<>();

        map.forEach((k, v) ->{
            Attribute attribute;
            switch (v){
                case "integer":
                    attribute = AttributeFactory.createAttribute(k, Ontology.INTEGER);
                    break;
                case "double":
                    attribute = AttributeFactory.createAttribute(k, Ontology.REAL);
                    break;
                case "date":
                    attribute = AttributeFactory.createAttribute(k, Ontology.DATE);
                    break;
                default:
                    attribute = AttributeFactory.createAttribute(k, Ontology.POLYNOMINAL);
                    break;

            }
            attributes.add(attribute);
        });

        ExampleSetBuilder builder = ExampleSets.from(attributes);
        double[] doubleArray = new double[attributes.size()];
        for(List<Object> list : data){
            for(int i=0; i<list.size(); ++i) {
                Attribute attribute = attributes.get(i);
                String attributeType = Ontology.ATTRIBUTE_VALUE_TYPE.mapIndex(attribute.getValueType());
                switch (attributeType) {
                    case "real":
                    case "integer":
                        try {
                            doubleArray[i] = Double.valueOf(String.valueOf(list.get(i)));
                        }catch (Exception e){
                            throw new Exception("数值形式数据错误");
                        }
                        break;
                    case "date":
                        try {
                            doubleArray[i] = ((Date) list.get(i)).getTime();
                        }catch (Exception e){
                            throw new Exception("时间形式数据错误");
                        }
                        break;
                    case "polynominal":
                    default:
                        doubleArray[i] = attribute.getMapping().mapString(String.valueOf(list.get(i)));
                        break;
                }
            }
            builder.addRow(doubleArray);
        }

        return builder.build();
    }

    public static void main(String[] args) {
        Map<String, String> map = new HashMap<>();
        map.put("a1", "integer");
        map.put("a2", "bool");
        map.put("a3", "double");
        map.put("a4", "string");
        map.put("a5", "date");



        List<List<Object> > lists = new ArrayList<>();
        List<Object> list = new ArrayList<>();
        list.add(1);
        list.add("sunny");
        list.add(12.2);
        list.add("aaa");
        list.add(new Date());
        lists.add(list);
        list = new ArrayList<>();
        list.add(2);
        list.add("rain");
        list.add(12.2);
        list.add("bbb");
        list.add(new Date());
        lists.add(list);
        list = new ArrayList<>();
        list.add(3);
        list.add("rain");
        list.add(14.2);
        list.add("ccc");
        list.add(new Date());
        lists.add(list);

        ExampleSet examples = null;
        try {
            examples = createExampleSet(map, lists);
        } catch (Exception e) {
            e.printStackTrace();
        }
        System.out.println(examples.getAttributes());
        System.out.println(examples.size());
        System.out.println(examples.getExample(0).get("a1"));
        System.out.println(examples.getExample(0).get("a5"));
        System.out.println(examples.getExample(1));
        System.out.println(examples.getExample(2));

    }
}
