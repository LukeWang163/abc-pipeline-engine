package base.operators.h2o;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;
import java.lang.reflect.Type;
import water.Key;

public class KeyTypeAdapter extends Object implements JsonSerializer<Key>, JsonDeserializer<Key> {
    @Override
    public JsonElement serialize(Key src, Type typeOfSrc, JsonSerializationContext context) { return new JsonPrimitive(src.toString()); }

    @Override
    public Key deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException { return Key.make(json.getAsString()); }
}

