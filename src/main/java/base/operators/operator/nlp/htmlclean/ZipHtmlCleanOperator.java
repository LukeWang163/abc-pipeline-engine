package base.operators.operator.nlp.htmlclean;

import base.operators.example.Attribute;
import base.operators.example.ExampleSet;
import base.operators.example.set.SimpleExampleSet;
import base.operators.example.table.AttributeFactory;
import base.operators.example.table.DataRow;
import base.operators.example.table.DataRowFactory;
import base.operators.example.table.MemoryExampleTable;
import base.operators.operator.Operator;
import base.operators.operator.OperatorDescription;
import base.operators.operator.OperatorException;
import base.operators.operator.nio.file.FileObject;
import base.operators.operator.ports.InputPort;
import base.operators.operator.ports.OutputPort;
import base.operators.tools.Ontology;
import org.apache.commons.lang.StringUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.safety.Whitelist;
import org.jsoup.select.Elements;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public class ZipHtmlCleanOperator extends Operator {

    private InputPort zipFileInput = getInputPorts().createPort("zip file");
    private OutputPort exampleSetOutput = getOutputPorts().createPort("example set");

    public ZipHtmlCleanOperator(OperatorDescription description){
        super(description);
    }

    @Override
    public void doWork() throws OperatorException {
        InputStream stream = zipFileInput.getData(FileObject.class).openStream();//文件流

        // 构造输出表
        List<Attribute> attributeList = new ArrayList<>();
        Attribute file_name_attribute = AttributeFactory.createAttribute("file_name", Ontology.STRING);
        attributeList.add(file_name_attribute);
        Attribute title_attribute = AttributeFactory.createAttribute("title", Ontology.STRING);
        attributeList.add(title_attribute);
        Attribute author_attribute = AttributeFactory.createAttribute("author", Ontology.STRING);
        attributeList.add(author_attribute);
        Attribute content_attribute = AttributeFactory.createAttribute("content", Ontology.STRING);
        attributeList.add(content_attribute);
        Attribute url_attribute = AttributeFactory.createAttribute("url", Ontology.STRING);
        attributeList.add(url_attribute);

        MemoryExampleTable exampleTable = new MemoryExampleTable(attributeList);
        //解压缩文件并解析html文件
        ZipInputStream zin = new ZipInputStream(stream, Charset.forName("gbk"));
        ZipEntry ze;
        BufferedReader br = null;
        try{
            while ((ze = zin.getNextEntry()) != null) {
                if (ze.isDirectory()) {
                } else {
                    DataRowFactory factory = new DataRowFactory(0, '.');
                    DataRow dataRow = factory.create(attributeList.size());
                    dataRow.set(file_name_attribute, file_name_attribute.getMapping().mapString(ze.getName()));
                    long size = ze.getSize();
                    if (size > 0) {
                        br = new BufferedReader(new InputStreamReader(zin, "UTF-8"));
                        String line;
                        StringBuffer sb = new StringBuffer();
                        while ((line = br.readLine()) != null) {
                            sb.append(line);
                        }
                        Document doc = Jsoup.parse(sb.toString());
                        // 提取标题
                        Elements ti = doc.select("title");
                        String title = ti.text();
                        ti.remove();
                        // 提取备选标题2
                        Elements h = doc.select("h1");
                        String h1 = h.text();
                        h.remove();
                        // 提取作者
                        Elements au = doc.select("div[id=\"author\"]");
                        String author = au.text();
                        au.remove();
                        // 提取html链接
                        Pattern p = Pattern.compile("\"([https|http|ftp|rtsp|mms]+://[^\\s]*)\"");
                        Matcher m = p.matcher(sb.toString());
                        List<String> list1 = new ArrayList<String>();
                        while (m.find()) {
                            list1.add(m.group());
                        }
                        Set<String> set = new HashSet<String>(list1);
                        ArrayList<String> tempList = new ArrayList<String>(set);
                        String ulist = StringUtils.join(tempList, " ");
                        String urllist = ulist.replace(",", "").replace("\"", " ");
                        // 删除指定标签
                        Elements ty = doc.select("style[type=\"text/css\"]");
                        ty.remove();

                        // 提取正文p标签
                        Elements p1 = doc.select("p");
                        String p2 = p1.text().replace(",", "，");
                        // 提取正文td标签
                        // Elements td1 = doc.select("td");
                        // String td = td1.text();

                        // 调用jsoup清洗方法
                        String str2 = Jsoup.clean(doc.toString(), Whitelist.none());
                        String strtxt = remove(str2);

                        if (h1 != null && !h1.equals("")) {
                            dataRow.set(title_attribute, title_attribute.getMapping().mapString(h1));
                        } else {
                            dataRow.set(title_attribute, title_attribute.getMapping().mapString(title + " "));
                        }
                        dataRow.set(author_attribute, author_attribute.getMapping().mapString(author));

                        if (p2 != null && !p2.equals("")) {
                            dataRow.set(content_attribute, content_attribute.getMapping().mapString(p2));
                        } else {
                            dataRow.set(content_attribute, content_attribute.getMapping().mapString(strtxt));
                        }
                        dataRow.set(url_attribute, url_attribute.getMapping().mapString(urllist));
                    }
                    exampleTable.addDataRow(dataRow);

                }
            }
            br.close();
        }catch (IOException e){
            e.printStackTrace();
        }
        ExampleSet exampleSet = new SimpleExampleSet(exampleTable);
        exampleSetOutput.deliver(exampleSet);
    }


    private static String remove(String inputString) {
        String htmlStr = inputString;
        String textStr = "";
        try {
            String regEx_special = "[\\[0-9\\]]*\\&[a-zA-Z]{1,10};";

            String regEx_zifu = ("[`~!@#$^&*()=|{}':;',\\[\\].<>/?~]");

            Pattern p_special = Pattern.compile(regEx_special);
            Matcher m_special = p_special.matcher(htmlStr);
            htmlStr = m_special.replaceAll("");

            Pattern p_zifu = Pattern.compile(regEx_zifu);
            Matcher m_zifu = p_zifu.matcher(htmlStr);
            htmlStr = m_zifu.replaceAll("");
            // 去掉八个字母以上的英文句子和标点（仅针对中文文章）
            String text1 = htmlStr.replaceAll("[\\w\\s|`~!-@#$^&*()=|\\\\{}':;\",\\[\\].<>/?]{8,}", "");
            String text2 = text1.replaceAll("\\s*|\\t*|\\r", "");
            textStr = text2;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return textStr;
    }
}
