package base.operators.operator.nlp.sentiment.gradeSentiment.entity;

import java.util.Comparator;

public class EntityComparator implements Comparator {
    @Override
    public int compare(Object obj0, Object obj1) {
        Entity entity0 = (Entity) obj0;
        Entity entity1 = (Entity) obj1;
        return entity0.startIndex > entity1.startIndex ? 1 : entity0.startIndex < entity1.startIndex ? -1 : entity0.entityLength > entity1.entityLength ? 1 : -1;
    }

//    public static void main(String[] args){
//        List<Entity> entityList = new ArrayList<>();
//        Entity en1 = new Entity("美丽", 3, "sentiment");
//        Entity en2 = new Entity("一点也不", 8,"not");
//        Entity en3 = new Entity("美丽大方",3,"sentiment");
//        entityList.add(en1);
//        entityList.add(en2);
//        entityList.add(en3);
//        EntityComparator compara = new EntityComparator();
//        Collections.sort(entityList, compara);
//        System.out.println(entityList.get(0).entityName);
//        System.out.println(entityList.get(1).entityName);
//        System.out.println(entityList.get(2).entityName);
//    }

}
