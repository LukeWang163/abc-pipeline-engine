package base.operators.operator.nlp.sentiment.gradeSentiment.entity;

public class Entity {
    //实体名称
    public String entityName;
    //实体的起始索引
    public Integer startIndex;
    //实体的结束索引
    public Integer endIndex;
    //实体的类型
    public String entityType;
    //实体的长度
    public Integer entityLength;

    public Entity(){
        entityName = "";
        startIndex = 0;
        endIndex = 0;
        entityType = "";
        entityLength = 0;

    }
    public Entity(String entityName, Integer startIndex,  String entityType){
        this.entityName = entityName;
        this.startIndex = startIndex;
        this.endIndex = startIndex + entityName.length();
        this.entityType = entityType;
        this.entityLength = entityName.length();
    }
    public void setEntityName(String entityName){
        this.entityName = entityName;
    }
    public String getEntityName(){
        return entityName;
    }

    public void setStartIndex(Integer startIndex){
        this.startIndex = startIndex;
    }
    public Integer getStartIndex(){
        return startIndex;
    }

    public void setEndIndex(Integer endIndex){
        this.endIndex = endIndex;
    }

    public Integer getEndIndex(){
        return endIndex;
    }

    public void setEntityType(String entityType){
        this.entityType = entityType;
    }
    public String getEntityType(){
        return entityType;
    }

    public void setEntityLength(Integer entityLength){
        this.entityLength = entityLength;
    }

    public Integer getEntityLength(){
        return entityLength;
    }

}
