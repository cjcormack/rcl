package org.netkernelroc.lang.rcl.endpoint;

import org.netkernel.layer0.nkf.*;
import org.netkernel.layer0.util.RequestBuilder;
import org.netkernel.layer0.util.XMLReadable;
import org.netkernel.layer0.util.XMLUtils;
import org.netkernel.layer0.util.XPath;
import org.netkernel.module.standard.endpoint.StandardAccessorImpl;
import org.netkernel.util.Utils;
import org.w3c.dom.*;

import javax.xml.parsers.ParserConfigurationException;
import java.util.List;

/**
 * RCL Runtime endpoint
 *
 * @author Randolph Kahle
 */
public class RCLRuntime extends StandardAccessorImpl
  {
  private static final String RCL_NS = "http://netkernelroc.org/rcl";
  private static final String ARGUMENT_ATT = "argument-";

  public RCLRuntime()
    {
    declareThreadSafe();
    }


  public void onSource(INKFRequestContext context) throws Exception
    {
    boolean tolerant = context.getThisRequest().argumentExists("tolerant");
    String mimeType = context.getThisRequest().getArgumentValue("mimeType");

    Node templateNode = context.source("arg:template", Node.class);

    // Make a copy so we don't change an immutable resource representation
    Document template = getMutableClone(templateNode);
    processTemplate(template.getDocumentElement(), context, tolerant);

    INKFResponse response = context.createResponseFrom(template);

    if (mimeType != null)
      {
      response.setMimeType(mimeType);
      }
    else
      {
      response.setMimeType("application/xml");
      }
    }




  //========== Private ==========

  private void processTemplate(Element documentElement, INKFRequestContext context, boolean tolerant) throws Exception
    {
    String rclns = documentElement.getAttribute("xmlns:xrl");
    // Remove the namespace attribute from the processed node
    if (rclns.length() > 0)
      {
      documentElement.removeAttribute("xmlns:rcl");
      }

    Element e = XMLUtils.getFirstChildElement(documentElement);
    while (e != null)
      {
      Element next = XMLUtils.getNextSiblingElement(e);
      String ns = e.getNamespaceURI();
      if (ns != null && ns.equals(RCL_NS))
        {
        String name = e.getLocalName();
        if (name.equals("include"))
          {
          processInclude(e, context, tolerant);
          }
//        else if (name.equals("include-children"))
//          { //TODO rsk do you really want this - what is the use case?
//          processInclude(e, context, tolerant);
//          }
        else if (name.equals("resolve"))
          {
//          processResolve(e, context, tolerant);
          }
        else if (name.equals("eval"))
          {
//          processEval(e, context, tolerant);
          }
        }
      else
        {
        String inlineResolve = e.getAttribute("rcl:resolve");
        if (inlineResolve.length() > 0)
          {
//          processInlineResolve(e, inlineResolve, context, tolerant);
          }
        String inlineEval = e.getAttribute("rcl:eval");
        if (inlineEval.length() > 0)
          {
//          processInlineEval(e, inlineEval, context, tolerant);
          }
        processTemplate(e, context, tolerant);
        }
      e = next;
      }


    }

  /**
   * Called when an rcl:include tag is discovered in the template
   *
   */
  private void processInclude(Element element, INKFRequestContext context, boolean tolerant) throws Exception
    {
    String target = null;
    try
      {
      INKFRequest request;
      Element toReplace = element;

      boolean isInline = XMLUtils.getChildElementNamed(element, "xpath") == null;
      if (isInline)
        {
        Element requestElement = XMLUtils.getChildElementNamed(element, "request");
        XMLReadable r = new XMLReadable(requestElement);
        request = buildRequest(r, context);

//        if (element.hasAttribute("request"))
//          {
//          target = element.getAttribute("identifier");
//          request = context.createRequest(target);
//          }
//        else
//          {
//          exceptionHandler(context, tolerant, "EX_INCLUDE", "MSG_NO_BASE_OR_TARGET", element, null, target);
//          return;
//          }
        processArgumentAttributes(request, element);
        }
      else
        {
        XMLReadable r = new XMLReadable(element);
        request = buildRequest(r, context);

        String xpath = r.getTrimText("xrl:xpath");
        if (xpath.length() > 0)
          {
          List<Node> nodes = XPath.eval(xpath, element);
          if (nodes.size() == 1 && nodes.get(0) instanceof Element)
            {
            toReplace = (Element) nodes.get(0);
            }
          else
            {
            exceptionHandler(context, tolerant, "EX_INCLUDE", "MSG_BAD_XPATH", element, null, target);
            }
          }
        }

      request.setRepresentationClass(Node.class);
      Node n = (Node) context.issueRequest(request);
      if (n == null)
        {
        exceptionHandler(context, tolerant, "EX_INCLUDE", "MSG_NULL", element, null, target);
        return;
        }
      else if (n instanceof Document)
        {
        n = ((Document) n).getDocumentElement();
        }
      Element frag = (Element) element.getOwnerDocument().importNode(n, true);
      toReplace.getParentNode().replaceChild(frag, toReplace);
      if (toReplace != element)
        {
        element.getParentNode().removeChild(element);
        }
      processTemplate(frag, context, tolerant);
      }
    catch (Exception e)
      {
      exceptionHandler(context, tolerant, "EX_INCLUDE", "MSG_EVAL", element, e, target);
      }
    }

  private void exceptionHandler(INKFRequestContext aHelper, boolean tolerant, String aException, String aMessage, Node aElement, Exception e, String target) throws Exception
    {
    if (tolerant)
      {
      aHelper.logFormatted(INKFLocale.LEVEL_WARNING, aMessage, XMLUtils.getPathFor(aElement), e, target);
      if (e != null)
        {
        aHelper.logRaw(INKFLocale.LEVEL_WARNING, Utils.throwableToString(e));
        }
      }
    else
      {
      throw aHelper.createFormattedException(aException, aMessage, XMLUtils.getPathFor(aElement), e, target);
      }
    }

  /**
   * Build and request a request given an rcl:request element
   *
   * @param aReadable
   * @param aContext
   * @return
   * @throws Exception
   */
  private INKFRequest buildRequest(XMLReadable aReadable, INKFRequestContext aContext) throws Exception
    {
    Document d = XMLUtils.newDocument();
    Element docEl = d.createElement("request");
    d.appendChild(docEl);
    String identifier = aReadable.getTrimText("rcl:identifier");
    Element idEl = d.createElement("identifier");
    XMLUtils.setText(idEl, identifier);
    docEl.appendChild(idEl);
    List<Element> arguments = (List<Element>) (List) aReadable.getNodes("xrl:argument");
    for (Element argument : arguments)
      {
      Element argEl = d.createElement("argument");
      NamedNodeMap nnm = argument.getAttributes();
      for (int i = 0; i < nnm.getLength(); i++)
        {
        Node attIn = nnm.item(i);
        argEl.setAttribute(attIn.getLocalName(), attIn.getNodeValue());
        }
      for (Node n = argument.getFirstChild(); n != null; n = n.getNextSibling())
        {
        argEl.appendChild(d.importNode(n, true));
        }
      docEl.appendChild(argEl);
      }

    RequestBuilder b = new RequestBuilder(docEl, aContext.getKernelContext().getKernel().getLogger());
    INKFRequest req = b.buildRequest(aContext, null, null);
    return req;
    }


  private void processArgumentAttributes(INKFRequest aReq, Element aElement) throws NKFException
    {
    NamedNodeMap nnm = aElement.getAttributes();
    for (int i = nnm.getLength() - 1; i >= 0; i--)
      {
      Node n = nnm.item(i);
      String argName = ("xrl".equals(n.getPrefix())) ? n.getLocalName() : n.getNodeName();
      if (argName.startsWith(ARGUMENT_ATT))
        {
        String name = argName.substring(ARGUMENT_ATT.length());
        String value = n.getNodeValue();
        try
          {
          aReq.addArgument(name, value);
          }
        catch (NKFException e)
          {  //argument not available so don't add it- if mandatory then grammar will catch it
          if (!isArg(value))
            {
            throw e;
            }
          }
        aElement.removeAttributeNode((Attr) n);
        }
      }
    }

  private static boolean isArg(String aIdentifier)
    {
    return aIdentifier.startsWith("arg:");
    }


  private Document getMutableClone(Node node) throws ParserConfigurationException
    {
    Document result;
    if (node instanceof Document)
      {
      result = (Document) XMLUtils.safeDeepClone(node);
      }
    else
      {
      result = org.netkernel.layer0.util.XMLUtils.newDocument();
      result.appendChild(result.importNode(node, true));
      }
    return result;
    }


  }
