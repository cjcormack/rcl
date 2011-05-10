package org.netkernelroc.lang.rcl.endpoint;


import java.util.HashMap;
import java.util.Map;

public class TagProcessorRegistry
  {
  private static TagProcessorRegistry tagProcessorRegistry = null;

  private Map<String, ITagProcessor> tagProcessors = new HashMap<String, ITagProcessor>();

  private TagProcessorRegistry()
    {
    tagProcessors.put("if", new IfTagProcessor());
    tagProcessors.put("include", new IncludeTagProcessor());
    }

  public static ITagProcessor getTagProcessor(String tagName)
    {
    if (tagProcessorRegistry == null)
      {
      tagProcessorRegistry = new TagProcessorRegistry();
      }
    return tagProcessorRegistry.tagProcessors.get(tagName);
    }


  }
