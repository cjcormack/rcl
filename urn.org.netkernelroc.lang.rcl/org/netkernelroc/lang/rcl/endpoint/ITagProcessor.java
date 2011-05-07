package org.netkernelroc.lang.rcl.endpoint;

import nu.xom.Node;
import org.netkernel.layer0.nkf.INKFRequestContext;
import org.netkernel.layer0.nkf.NKFException;

import java.util.List;

/**
 * Defines plugin tag provider
 *
 */
public interface ITagProcessor
  {


  public List<Node> processChildren(List<Node> elements, INKFRequestContext context, boolean tolerant) throws NKFException;


  }
