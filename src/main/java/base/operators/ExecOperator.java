package base.operators;

import base.operators.utils.ExecOperatorImpl;
import base.operators.utils.ExecProcessImpl;
import com.alibaba.fastjson.JSONObject;

import java.io.UnsupportedEncodingException;
import java.util.Base64;

public class ExecOperator {


        //inputExtenderOperators.add("base.operators.operator.preprocessing.join.ExampleSetMerge");

        //outputExtenderOperators.add("base.operators.operator.IOMultiplier");

        //pairExtenderOperators.add("base.operators.operator.validation.significance.TTestSignificanceTestOperator");
        //pairExtenderOperators.add("base.operators.operator.validation.significance.AnovaSignificanceTestOperator");


    public static void main(String[] args) {

        String operatorParams = args[0];
//        String operatorParams = "eyJmdWxsTmFtZSI6ImJhc2Uub3BlcmF0b3JzLm9wZXJhdG9yLnByZXByb2Nlc3NpbmcuZmlsdGVyLkV4YW1wbGVTZXRUb0RpY3Rpb25hcnkiLCJpbnB1dHMiOlt7InR5cGUiOiJkYXRhIiwicGF0aCI6Ii9kYXRhL2V4cGVyaW1lbnQvZGF0YS8xNTY5Mjg5MDM5OTExXzEiLCJzcmNOb2RlTmFtZSI6Im5ld19yZWFkX2NzdiIsInNlcXVlbmNlIjoxfSx7InR5cGUiOiJkYXRhIiwicGF0aCI6Ii9kYXRhL2V4cGVyaW1lbnQvZGF0YS8xNTY5Mjg5MDk3OTk5XzEiLCJzcmNOb2RlTmFtZSI6ImZpbHRlcl9leGFtcGxlX3JhbmdlIiwic2VxdWVuY2UiOjJ9XSwib3V0cHV0cyI6W3sidHlwZSI6ImRhdGEiLCJwYXRoIjoiL2RhdGEvZXhwZXJpbWVudC9kYXRhLzE1NjkyODkwNTc4MzJfMSJ9LHsidHlwZSI6ImRhdGEiLCJwYXRoIjoiL2RhdGEvZXhwZXJpbWVudC9kYXRhLzE1NjkyODkwNTc4MzJfMiJ9LHsidHlwZSI6ImRhdGEiLCJwYXRoIjoiL2RhdGEvZXhwZXJpbWVudC9kYXRhLzE1NjkyODkwNTc4MzJfMyJ9XSwicGFyYW1zIjp7ImZyb21fYXR0cmlidXRlIjoiYXR0MSIsInZhbHVlX3R5cGUiOiJhdHRyaWJ1dGVfdmFsdWUiLCJibG9ja190eXBlIjoiYXR0cmlidXRlX2Jsb2NrIiwicmVndWxhcl9leHByZXNzaW9uIjoiIiwidXNlX2V4Y2VwdF9leHByZXNzaW9uIjoiZmFsc2UiLCJjb252ZXJ0X3RvX2xvd2VyY2FzZSI6ImZhbHNlIiwidG9fYXR0cmlidXRlIjoiYXR0MiIsInVzZV9yZWd1bGFyX2V4cHJlc3Npb25zIjoiZmFsc2UiLCJleGNlcHRfYmxvY2tfdHlwZSI6InZhbHVlX21hdHJpeF9yb3dfc3RhcnQiLCJudW1lcmljX2NvbmRpdGlvbiI6IiIsImluY2x1ZGVfc3BlY2lhbF9hdHRyaWJ1dGVzIjoiZmFsc2UiLCJhdHRyaWJ1dGVfZmlsdGVyX3R5cGUiOiJhbGwiLCJmaXJzdF9tYXRjaF9vbmx5IjoiZmFsc2UiLCJ1c2VfdmFsdWVfdHlwZV9leGNlcHRpb24iOiJmYWxzZSIsImV4Y2VwdF92YWx1ZV90eXBlIjoidGltZSIsInVzZV9ibG9ja190eXBlX2V4Y2VwdGlvbiI6ImZhbHNlIiwiZXhjZXB0X3JlZ3VsYXJfZXhwcmVzc2lvbiI6IiIsImF0dHJpYnV0ZSI6IiIsImNyZWF0ZV92aWV3IjoiZmFsc2UiLCJpbnZlcnRfc2VsZWN0aW9uIjoiZmFsc2UifSwieG1sUGF0aCI6IiJ9";
//        String operatorParams = "eyJmdWxsTmFtZSI6ImJhc2Uub3BlcmF0b3JzLm9wZXJhdG9yLk1vZGVsQXBwbGllciIsImlucHV0cyI6W3sidHlwZSI6ImZpbGUiLCJwYXRoIjoiL2RhdGEvZXhwZXJpbWVudC9maWxlLzE1Njg4NTU1ODI5OThfMSIsInNyY05vZGVOYW1lIjoiZGVmYXVsdF9tb2RlbCIsInNlcXVlbmNlIjoxfSx7InR5cGUiOiJkYXRhIiwicGF0aCI6Ii9kYXRhL2V4cGVyaW1lbnQvZGF0YS8xNTY4ODU1NTcwMzU0XzIiLCJzcmNOb2RlTmFtZSI6InNwbGl0X2RhdGEiLCJzZXF1ZW5jZSI6Mn1dLCJvdXRwdXRzIjpbeyJ0eXBlIjoiZGF0YSIsInBhdGgiOiIvZGF0YS9leHBlcmltZW50L2RhdGEvMTU2ODg1NTgxMTg2N18xIn0seyJ0eXBlIjoiZmlsZSIsInBhdGgiOiIvZGF0YS9leHBlcmltZW50L2ZpbGUvMTU2ODg1NTgxMTg2N18yIn1dLCJwYXJhbXMiOnsiYXBwbGljYXRpb25fcGFyYW1ldGVycyI6IiIsImNyZWF0ZV92aWV3IjoiZmFsc2UifSwieG1sUGF0aCI6IiJ9";
//        String envParams = args[1];

        // 本机测试
        // ReadCSV
//        String operatorParams = "ewogICJmdWxsTmFtZSI6ICJiYXNlLm9wZXJhdG9ycy5vcGVyYXRvci5sZWFybmVyLmJheWVzLk5haXZlQmF5ZXMiLAogICJleGVjdXRlUGF0aCI6ICJkYXRhL2ZpbGUyaGl2ZSIsCiAgImlucHV0cyI6IFsKICAgIHsKICAgICAgInR5cGUiOiAiZGF0YSIsCiAgICAgICJwYXRoIjogIi9kYXRhL3JhcGlkbWluZXJUZXN0L3NldF9yb2xlL1RpdGFuaWNfc2V0X3JvbGUyIgogICAgfQogIF0sCiAgIm91dHB1dHMiOiBbCiAgICB7CiAgICAgICJ0eXBlIjogIm1vZGVsIiwKICAgICAgInBhdGgiOiAiL2RhdGEvcmFwaWRtaW5lclRlc3QvbW9kZWwvVGl0YW5pY19uYWl2ZV9iYXllc19tb2RlbCIKICAgIH0KICBdLAogICJwYXJhbXMiOiB7CgogIH0KfQ==";
//        String operatorParams = "eyJmdWxsTmFtZSI6ImJhc2Uub3BlcmF0b3JzLm9wZXJhdG9yLm5pby5DU1ZFeGFtcGxlU291cmNlIiwiaW5wdXRzIjpbXSwib3V0cHV0cyI6W3sidHlwZSI6ImRhdGEiLCJwYXRoIjoiL2RhdGEvZXhwZXJpbWVudC9kYXRhLzE1NjgwMTc0NjI2NTFfMSJ9XSwicGFyYW1zIjp7ImNzdl9maWxlIjoiL2RhdGEvcmFwaWRtaW5lclRlc3QvY3N2L0lyaXMuY3N2IiwiY29sdW1uX3NlcGFyYXRvcnMiOiIsIn0sInhtbFBhdGgiOiIifQ==";
//        String operatorParams = "eyJmdWxsTmFtZSI6ImJhc2Uub3BlcmF0b3JzLm9wZXJhdG9yLnByZXByb2Nlc3NpbmcuZmlsdGVyLkNoYW5nZUF0dHJpYnV0ZVJvbGUiLCJpbnB1dHMiOlt7InR5cGUiOiJkYXRhIiwicGF0aCI6Ii9kYXRhL2V4cGVyaW1lbnQvZGF0YS8xNTY4MDE3NDYyNjUxXzEiLCJzcmNOb2RlTmFtZSI6InJlYWRfY3N2Iiwic2VxdWVuY2UiOjF9XSwib3V0cHV0cyI6W3sidHlwZSI6ImRhdGEiLCJwYXRoIjoiL2RhdGEvZXhwZXJpbWVudC9kYXRhLzE1NjgwMTc0NjM3OTlfMSJ9LHsidHlwZSI6ImRhdGEiLCJwYXRoIjoiL2RhdGEvZXhwZXJpbWVudC9kYXRhLzE1NjgwMTc0NjM3OTlfMiJ9XSwicGFyYW1zIjp7ImF0dHJpYnV0ZV9uYW1lIjoic3BlY2llcyIsInRhcmdldF9yb2xlIjoibGFiZWwifSwieG1sUGF0aCI6IiJ9";
//        String operatorParams = "ewogICAgImZ1bGxOYW1lIjogImJhc2Uub3BlcmF0b3JzLm9wZXJhdG9yLm5pby5DU1ZFeGFtcGxlU291cmNlIiwKICAgICJleGVjdXRlUGF0aCI6ICJkYXRhL2ZpbGUyaGl2ZSIsCiAgICAiaW5wdXRzIjogW10sCiAgICAib3V0cHV0cyI6IFsKICAgICAgICB7CiAgICAgICAgICAgICJ0eXBlIjogImRhdGEiLAogICAgICAgICAgICAicGF0aCI6ICJFOi9kYXRhLzE1NjM0MTE2MjQ1ODFfMSIKICAgICAgICB9CiAgICBdLAogICAgInBhcmFtcyI6IHsKICAgICAgICAiY3N2X2ZpbGUiOiAiRTovZGF0YS9JcmlzLmNzdiIsCiAgICAgICAgImNvbHVtbl9zZXBhcmF0b3JzIjoiLCIKICAgIH0KfQ==";
//        String operatorParams = "ewogICAgImZ1bGxOYW1lIjogImJhc2Uub3BlcmF0b3JzLm9wZXJhdG9yLm5pby5DU1ZFeGFtcGxlU291cmNlIiwKICAgICJleGVjdXRlUGF0aCI6ICJkYXRhL2ZpbGUyaGl2ZSIsCiAgICAiaW5wdXRzIjogW10sCiAgICAib3V0cHV0cyI6IFsKICAgICAgICB7CiAgICAgICAgICAgICJ0eXBlIjogImRhdGEiLAogICAgICAgICAgICAicGF0aCI6ICIvZGF0YS9yYXBpZG1pbmVyLzE1NjM0MTE2MjQ1ODFfMSIKICAgICAgICB9CiAgICBdLAogICAgInBhcmFtcyI6IHsKICAgICAgICAiY3N2X2ZpbGUiOiAiRTovZGF0YS9JcmlzLmNzdiIsCiAgICAgICAgImNvbHVtbl9zZXBhcmF0b3JzIjoiLCIKICAgIH0KfQ==";
        // setRole
//        String operatorParams = "ewogICAgImZ1bGxOYW1lIjogImJhc2Uub3BlcmF0b3JzLm9wZXJhdG9yLnByZXByb2Nlc3NpbmcuZmlsdGVyLkNoYW5nZUF0dHJpYnV0ZVJvbGUiLAogICAgImV4ZWN1dGVQYXRoIjogImRhdGEvZmlsZTJoaXZlIiwKICAgICJpbnB1dHMiOiBbCiAgICAgICAgewogICAgICAgICAgICAidHlwZSI6ICJkYXRhIiwKICAgICAgICAgICAgInBhdGgiOiAiRTovZGF0YS8xNTYzNDExNjI0NTgxXzEiCiAgICAgICAgfQogICAgXSwKICAgICJvdXRwdXRzIjogWwogICAgICAgIHsKICAgICAgICAgICAgInR5cGUiOiAiZGF0YSIsCiAgICAgICAgICAgICJwYXRoIjogIkU6L2RhdGEvMTU2MzQxMTYyNDU4MV8yIgogICAgICAgIH0sCiAgICAgICAge30KICAgIF0sCiAgICAicGFyYW1zIjogewogICAgICAgICJhdHRyaWJ1dGVfbmFtZSI6ICJzcGVjaWVzIiwKICAgICAgICAidGFyZ2V0X3JvbGUiOiAibGFiZWwiCiAgICB9Cn0=";
        //String operatorParams = "ewogICAgImZ1bGxOYW1lIjogImJhc2Uub3BlcmF0b3JzLm9wZXJhdG9yLnByZXByb2Nlc3NpbmcuZmlsdGVyLkNoYW5nZUF0dHJpYnV0ZVJvbGUiLAogICAgImV4ZWN1dGVQYXRoIjogImRhdGEvZmlsZTJoaXZlIiwKICAgICJpbnB1dHMiOiBbCiAgICAgICAgewogICAgICAgICAgICAidHlwZSI6ICJkYXRhIiwKICAgICAgICAgICAgInBhdGgiOiAiL2RhdGEvcmFwaWRtaW5lci8xNTYzNDExNjI0NTgxXzEiCiAgICAgICAgfQogICAgXSwKICAgICJvdXRwdXRzIjogWwogICAgICAgIHsKICAgICAgICAgICAgInR5cGUiOiAiZGF0YSIsCiAgICAgICAgICAgICJwYXRoIjogIi9kYXRhL3JhcGlkbWluZXIvMTU2MzQxMTYyNDU4MV8yIgogICAgICAgIH0sCiAgICAgICAge30KICAgIF0sCiAgICAicGFyYW1zIjogewogICAgICAgICJhdHRyaWJ1dGVfbmFtZSI6ICJzcGVjaWVzIiwKICAgICAgICAidGFyZ2V0X3JvbGUiOiAibGFiZWwiCiAgICB9Cn0=";
        // libSVM
//        String operatorParams="ewogICAgImZ1bGxOYW1lIjogImJhc2Uub3BlcmF0b3JzLm9wZXJhdG9yLmxlYXJuZXIuZnVuY3Rpb25zLmtlcm5lbC5MaWJTVk1MZWFybmVyIiwKICAgICJleGVjdXRlUGF0aCI6ICJkYXRhL2ZpbGUyaGl2ZSIsCiAgICAiaW5wdXRzIjogWwogICAgICAgIHsKICAgICAgICAgICAgInR5cGUiOiAiZGF0YSIsCiAgICAgICAgICAgICJwYXRoIjogIkU6L2RhdGEvMTU2MzQxMTYyNDU4MV8yIgogICAgICAgIH0KICAgIF0sCiAgICAib3V0cHV0cyI6IFsKICAgICAgICB7CiAgICAgICAgICAgICJ0eXBlIjogIm1vZGVsIiwKICAgICAgICAgICAgInBhdGgiOiAiRTovZGF0YS8xNTYzNDExNjI0NTgxXzMiCiAgICAgICAgfSwKICAgICAgICB7CiAgICAgICAgICAgICJ0eXBlIjogImRhdGEiLAogICAgICAgICAgICAicGF0aCI6ICJFOi9kYXRhLzE1NjM0MTE2MjQ1ODFfNCIKICAgICAgICB9CiAgICBdLAogICAgInBhcmFtcyI6IHt9Cn0=";
//          String operatorParams="ewogICAgImZ1bGxOYW1lIjogImJhc2Uub3BlcmF0b3JzLm9wZXJhdG9yLmxlYXJuZXIuZnVuY3Rpb25zLmtlcm5lbC5MaWJTVk1MZWFybmVyIiwKICAgICJleGVjdXRlUGF0aCI6ICJkYXRhL2ZpbGUyaGl2ZSIsCiAgICAiaW5wdXRzIjogWwogICAgICAgIHsKICAgICAgICAgICAgInR5cGUiOiAiZGF0YSIsCiAgICAgICAgICAgICJwYXRoIjogIi9kYXRhL3JhcGlkbWluZXIvMTU2MzQxMTYyNDU4MV8yIgogICAgICAgIH0KICAgIF0sCiAgICAib3V0cHV0cyI6IFsKICAgICAgICB7CiAgICAgICAgICAgICJ0eXBlIjogIm1vZGVsIiwKICAgICAgICAgICAgInBhdGgiOiAiL2RhdGEvcmFwaWRtaW5lci8xNTYzNDExNjI0NTgxXzMiCiAgICAgICAgfSwKICAgICAgICB7CiAgICAgICAgICAgICJ0eXBlIjogImRhdGEiLAogICAgICAgICAgICAicGF0aCI6ICIvZGF0YS9yYXBpZG1pbmVyLzE1NjM0MTE2MjQ1ODFfNCIKICAgICAgICB9CiAgICBdLAogICAgInBhcmFtcyI6IHt9Cn0=";
        // Apply Model
//        String operatorParams = "ewogICAgImZ1bGxOYW1lIjogImJhc2Uub3BlcmF0b3JzLm9wZXJhdG9yLk1vZGVsQXBwbGllciIsCiAgICAiZXhlY3V0ZVBhdGgiOiAiZGF0YS9maWxlMmhpdmUiLAogICAgImlucHV0cyI6IFsKICAgICAgICB7CiAgICAgICAgICAgICJ0eXBlIjogIm1vZGVsIiwKICAgICAgICAgICAgInBhdGgiOiAiRTovZGF0YS8xNTYzNDExNjI0NTgxXzMiCiAgICAgICAgfSwKICAgICAgICB7CiAgICAgICAgICAgICJ0eXBlIjogImRhdGEiLAogICAgICAgICAgICAicGF0aCI6ICJFOi9kYXRhLzE1NjM0MTE2MjQ1ODFfNCIKICAgICAgICB9CiAgICBdLAogICAgIm91dHB1dHMiOiBbCiAgICAgICAgewogICAgICAgICAgICAidHlwZSI6ICJkYXRhIiwKICAgICAgICAgICAgInBhdGgiOiAiRTovZGF0YS8xNTYzNDExNjI0NTgxXzUiCiAgICAgICAgfSwKICAgICAgICB7CiAgICAgICAgICAgICJ0eXBlIjogIm1vZGVsIiwKICAgICAgICAgICAgInBhdGgiOiAiRTovZGF0YS8xNTYzNDExNjI0NTgxXzYiCiAgICAgICAgfQogICAgXSwKICAgICJwYXJhbXMiOiB7fQp9";
//        String operatorParams = "ewogICAgImZ1bGxOYW1lIjogImJhc2Uub3BlcmF0b3JzLm9wZXJhdG9yLk1vZGVsQXBwbGllciIsCiAgICAiZXhlY3V0ZVBhdGgiOiAiZGF0YS9maWxlMmhpdmUiLAogICAgImlucHV0cyI6IFsKICAgICAgICB7CiAgICAgICAgICAgICJ0eXBlIjogIm1vZGVsIiwKICAgICAgICAgICAgInBhdGgiOiAiL2RhdGEvcmFwaWRtaW5lci8xNTYzNDExNjI0NTgxXzMiCiAgICAgICAgfSwKICAgICAgICB7CiAgICAgICAgICAgICJ0eXBlIjogImRhdGEiLAogICAgICAgICAgICAicGF0aCI6ICIvZGF0YS9yYXBpZG1pbmVyLzE1NjM0MTE2MjQ1ODFfNCIKICAgICAgICB9CiAgICBdLAogICAgIm91dHB1dHMiOiBbCiAgICAgICAgewogICAgICAgICAgICAidHlwZSI6ICJkYXRhIiwKICAgICAgICAgICAgInBhdGgiOiAiL2RhdGEvcmFwaWRtaW5lci8xNTYzNDExNjI0NTgxXzUiCiAgICAgICAgfSwKICAgICAgICB7CiAgICAgICAgICAgICJ0eXBlIjogIm1vZGVsIiwKICAgICAgICAgICAgInBhdGgiOiAiL2RhdGEvcmFwaWRtaW5lci8xNTYzNDExNjI0NTgxXzYiCiAgICAgICAgfQogICAgXSwKICAgICJwYXJhbXMiOiB7fQp9";
        // hdfs测试
        // ReadCSV
//        String operatorParams = "ewogICAgImZ1bGxOYW1lIjogImJhc2Uub3BlcmF0b3JzLm9wZXJhdG9yLm5pby5DU1ZFeGFtcGxlU291cmNlIiwKICAgICJleGVjdXRlUGF0aCI6ICJkYXRhL2ZpbGUyaGl2ZSIsCiAgICAiaW5wdXRzIjogW10sCiAgICAib3V0cHV0cyI6IFsKICAgICAgICB7CiAgICAgICAgICAgICJ0eXBlIjogImRhdGEiLAogICAgICAgICAgICAicGF0aCI6ICIvZGF0YS9yYXBpZG1pbmVyL3Rlc3RfMSIKICAgICAgICB9CiAgICBdLAogICAgInBhcmFtcyI6IHsKICAgICAgICAiY3N2X2ZpbGUiOiAiRTovZGF0YS9JcmlzLmNzdiIsCiAgICAgICAgImNvbHVtbl9zZXBhcmF0b3JzIjogIiwiCiAgICB9Cn0=";

        // 1 Parse operatorParams
        try {
            operatorParams = new String(Base64.getDecoder().decode(operatorParams), "UTF-8");
//            envParams = new String(Base64.getDecoder().decode(envParams), "UTF-8");
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
//        String operatorParams = ReadJson.read("fast_k-means.json");
        JSONObject opParamsPairs = JSONObject.parseObject(operatorParams);
 //       String fullName = opParamsPairs.getString("fullName");// 入口类路径

//        if(inputExtenderOperators.contains(fullName)){
//            ExecOperatorImpl.execInputExtenderOperator(opParamsPairs);
//        }else if(outputExtenderOperators.contains(fullName)){
//            ExecOperatorImpl.execOutputExtenderOperator(opParamsPairs);
//        }else if(pairExtenderOperators.contains(fullName)){
//            ExecOperatorImpl.execpairExtenderOperator(opParamsPairs);
//        } else {
//            ExecOperatorImpl.execNormalOperator(opParamsPairs);
//        }

        String xmlPath = opParamsPairs.getString("xmlPath");
        if(xmlPath==null || "".equals(xmlPath)){
            ExecOperatorImpl.execOperator(opParamsPairs);
        }else {
            ExecProcessImpl.execute(opParamsPairs);
        }
//        System.exit(0);

    }


}
