package net.blay09.mods.balm.config;

import net.blay09.mods.balm.api.config.BalmConfigData;
import net.blay09.mods.balm.api.config.Comment;
import net.blay09.mods.balm.api.config.Config;
import net.blay09.mods.balm.api.config.ExpectedType;

import java.util.Arrays;
import java.util.List;

@Config("balm")
public class ExampleConfigData implements BalmConfigData {

    public static final String EXAMPLE_STATIC = "test";

    public enum ExampleEnum {
        Hello,
        World
    }

    @Comment("This is an example boolean property")
    public boolean exampleBoolean = true;
    public int exampleInt = 42;
    public String exampleString = "Hello World";
    public String exampleMultilineString = "Hello World";
    public ExampleEnum exampleEnum = ExampleEnum.Hello;
    @ExpectedType(String.class)
    public List<String> exampleStringList = Arrays.asList("Hello", "World");
    @ExpectedType(Integer.class)
    public List<Integer> exampleIntList = Arrays.asList(12, 24);
    @ExpectedType(ExampleEnum.class)
    public List<ExampleEnum> exampleEnumList = Arrays.asList(ExampleEnum.Hello, ExampleEnum.World);

    @Comment("This is an example category")
    public ExampleCategory exampleCategory = new ExampleCategory();

    public static class ExampleCategory {
        @Comment("This is an example string inside a category")
        public String innerField = "I am inside";
        public float exampleFloat = 42.84f;
    }
}
