package org.netkernelroc.utility.test;

import org.netkernel.layer0.nkf.INKFRequestContext;
import org.netkernel.layer0.nkf.INKFResponse;
import org.netkernel.module.standard.endpoint.StandardAccessorImpl;

/**
 * TrueFalse accessor returns a Boolean true or false value depending on how it is called.
 *
 * @author Randolph Kahle
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
