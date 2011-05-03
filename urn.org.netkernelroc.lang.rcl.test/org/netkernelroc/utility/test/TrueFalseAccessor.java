package org.netkernelroc.utility.test;

import org.netkernel.layer0.nkf.INKFRequestContext;
import org.netkernel.layer0.nkf.INKFResponse;
import org.netkernel.module.standard.endpoint.StandardAccessorImpl;

/**
 * Always returns a true or false Boolean depending on how it is called.
 *
 */
public class TrueFalseAccessor extends StandardAccessorImpl
  {

  public void onSource(INKFRequestContext context) throws Exception
    {
    Boolean returnValue;
    String argumentType = context.getThisRequest().getArgumentValue("type");


    if ("true".equals(argumentType))
      {
      returnValue = Boolean.TRUE;
      }
    else
      {
      returnValue = Boolean.FALSE;
      }
    context.createResponseFrom(returnValue).setExpiry(INKFResponse.EXPIRY_NEVER);
    }
  }
