package org.netkernelroc.lang.rcl.endpoint;

import nu.xom.Element;
import nu.xom.Node;
import org.netkernel.layer0.nkf.INKFRequestContext;
import org.netkernel.layer0.nkf.NKFException;

import java.util.ArrayList;
import java.util.List;

public class IncludeTagProcessor extends RCLRuntime
  {
  private static final String TAG_REQUEST = "request";

  public IncludeTagProcessor()
    {
    declareThreadSafe();
    }


  @Override
  public List<Node> processChildren(List<Node> childNodes, INKFRequestContext context, boolean tolerant) throws NKFException
    {
    List<Node> replacementNodes = new ArrayList<Node>();


    for (Node node: childNodes)
      {
      if (node instanceof Element)
        {
        Element element = (Element)node;
        if (element.getNamespaceURI(NS_RCL) != null)
          {
          if (TAG_REQUEST.equals(element.getLocalName()))
            {
            replacementNodes.add(processRequest(element, context, tolerant));
            }
          else
            {
            ITagProcessor tagProcessor = TagProcessorRegistry.getTagProcessor(element.getLocalName());
            if (tagProcessor != null)
              {
              List<Node> grandchildNodes = getChildNodes(element);
              element.removeChildren();
              replacementNodes.addAll(tagProcessor.processChildren(grandchildNodes, context, tolerant));
              }
            else
              {
              //Handle exception
              }

            }
          }
        else
          {
          element = processElement(element, context, tolerant);
          replacementNodes.add(element);
          }

        }
      else
        {
        replacementNodes.add(node);
        }
      }
    return replacementNodes;
    }
  }


//    for (Element child : childElements)
//      {
//      if (child.getNamespaceURI(NS_RCL) != null)
//        {
//        if (TAG_REQUEST.equals(child.getLocalName()))
//          {
//          replacementNodes.add(processRequest(child, context, tolerant));
//          }
//        else
//          {
//          ITagProcessor tagProcessor = TagProcessorRegistry.getTagProcessor(child.getLocalName());
//          if (tagProcessor != null)
//            {
//            List<Element> grandchildElements = getChildElements(child);
//            child.removeChildren();
//            replacementNodes.addAll(tagProcessor.processChildren(grandchildElements, context, tolerant));
//            }
//          else
//            {
//            //Handle exception
//            }
//
//          }
//        }
//      else
//        {
//        child = processElement(child, context, tolerant);
//        replacementNodes.add(child);
//        }
//      }
//
//    return replacementNodes;
//
//    }

