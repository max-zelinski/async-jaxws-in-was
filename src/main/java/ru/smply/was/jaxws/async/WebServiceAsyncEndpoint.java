package ru.smply.was.jaxws.async;

import org.apache.cxf.continuations.Continuation;
import org.apache.cxf.continuations.ContinuationProvider;

import javax.annotation.Resource;
import javax.jws.WebMethod;
import javax.jws.WebService;
import javax.xml.ws.WebServiceContext;
import javax.xml.ws.handler.MessageContext;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

@WebService(serviceName="WebServiceAsync")
public class WebServiceAsyncEndpoint {
    @Resource
    private WebServiceContext webServiceContext;

    private static ExecutorService pool = Executors.newCachedThreadPool();

    @WebMethod
    public String hello(int millis) throws Exception {
        Thread.sleep(millis);
        return "hello";
    }

    @WebMethod
    public String helloAsync(final int millis) throws Exception {
        System.out.println("helloAsync called");
        MessageContext messageContext = webServiceContext.getMessageContext();
        ContinuationProvider provider = (ContinuationProvider)messageContext.get(ContinuationProvider.class.getName());
        final Continuation continuation = provider.getContinuation();
        System.out.println("continuation.isNew " + continuation.isNew());

        if (continuation.isNew()) {
            System.out.println("new request");
            Future<String> future = pool.submit(new Callable<String>() {
                @Override
                public String call() throws Exception {
                    System.out.println("going to sleep");
                    Thread.sleep(millis);
                    System.out.println("wake up");
                    continuation.resume();
                    return "Hello from async!";
                }
            });

            continuation.setObject(future);
            System.out.println("suspending request");
            continuation.suspend(-1);
            System.out.println("suspended request");
            return "this should not be returned";
        }
        else {
            System.out.println("request resumed");
            Future<String> future = (Future<String>)continuation.getObject();

            // reset clears continuation object
            continuation.reset();
            return future.get();
        }
    }
}
