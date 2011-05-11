package org.netkernelroc.lang.rcl.endpoint;

import nu.xom.Element;
import nu.xom.Node;
import org.netkernel.layer0.nkf.INKFRequest;
import org.netkernel.layer0.nkf.INKFRequestContext;
import org.netkernel.layer0.nkf.NKFException;

import java.util.ArrayList;
import java.util.List;

public class IfTagProcessor extends RCLRuntime
  {
  private static final String TAG_TRUE = "true";
  private static final String TAG_FALSE = "false";


  public IfTagProcessor()
    {
    declareThreadSafe();
    }


  @Override
  public List<Node> processChildren(List<Node> childNodes, INKFRequestContext context, boolean tolerant) throws NKFException
    {
    boolean test = false;
    Element requestElement = null;

    // Find the request, issue it and then remove the node from the set of children
    for (Node n : childNodes)
      {
      if (n instanceof Element)
        {
        Element e = (Element) n;
        if (e.getNamespaceURI(NS_RCL) != null && TAG_REQUEST.equals(e.getLocalName()))
          {
          test = processRequestForBoolean(e, context, tolerant);
          requestElement = e;
          }
        }
      }
    if (requestElement != null)
      {
      childNodes.remove(requestElement);
      }

    List<Node> replacementNodes = new ArrayList<Node>();


    for (Node node : childNodes)
      {
      if (node instanceof Element)
        {
        Element element = (Element) node;

        if (element.getNamespaceURI(NS_RCL) != null)
          {
          if (TAG_TRUE.equals(element.getLocalName()))
            {
            if (test)
              {
              replacementNodes.addAll(processIfTrueFalse(element, context, tolerant));
              }
            }
          else if (TAG_FALSE.equals(element.getLocalName()))
            {
            if (!test)
              {
              replacementNodes.addAll(processIfTrueFalse(element, context, tolerant));
              }
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
              throw new NKFException("Unexpected RCL tag", "The tag <" + NS_RCL + ":" + element.getLocalName() + "> was encountered while processing rcl:if");
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


  public List<Node> processIfTrueFalse
      (
          final Element trueFalseElement,
          final INKFRequestContext context,
          boolean tolerant) throws NKFException
    {
    List<Node> replacementNodes = new ArrayList<Node>();

    List<Node> childNodes = getChildNodes(trueFalseElement);
    trueFalseElement.removeChildren();


    for (Node node : childNodes)
      {
      if (node instanceof Element)
        {
        Element element = (Element) node;

        if (element.getNamespaceURI(NS_RCL) != null)
          {
          ITagProcessor tagProcessor = TagProcessorRegistry.getTagProcessor(element.getLocalName());
          if (tagProcessor != null)
            {
            List<Node> grandchildElements = getChildNodes(element);
            element.removeChildren();
            replacementNodes.addAll(tagProcessor.processChildren(grandchildElements, context, tolerant));
            }
          else
            {
            //Handle exception
            throw new NKFException("Unexpected RCL tag", "The tag <" + NS_RCL + ":" + element.getLocalName() + "> was encountered while processing rcl:true or rcl:false");
            }
          }
        else
          {
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


  public boolean processRequestForBoolean(final Element requestElement, final INKFRequestContext context, boolean tolerant) throws NKFException
    {
    boolean returnValue = true;
    try
      {
      INKFRequest request = buildRequest(requestElement, context);
      request.setRepresentationClass(java.lang.Boolean.class);
      returnValue = (Boolean) context.issueRequest(request);
      }
    catch (NKFException e)
      {
      exceptionHandler(context, e, requestElement, tolerant);
      }
  return returnValue;
    }

  }
