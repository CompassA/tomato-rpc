package org.tomato.study.rpc.impl.proxy;

import com.itranswarp.compiler.JavaStringCompiler;
import org.tomato.study.rpc.core.AbstractStub;
import org.tomato.study.rpc.core.MsgSender;
import org.tomato.study.rpc.core.StubFactory;

import java.io.IOException;
import java.lang.reflect.Method;
import java.util.stream.Stream;

/**
 * Create RPC client stub by raw string implement
 * java array type is not supported
 * internal interface is not supported
 * @author Tomato
 * Created on 2021.03.31
 */
public class RawStubFactory implements StubFactory {

    private static final String STUB_PACKAGE = "org.tomato.study.rpc.impl";

    private static final String STUB_TEMPLATE = "package " + STUB_PACKAGE + ";\n" +
            "import org.tomato.study.rpc.core.AbstractStub;\n" +
            "import org.tomato.study.rpc.data.MethodContext;\n" +
            "\n" +
            "public class %s extends AbstractStub implements %s {\n" +
            "%s" +
            "}\n";

    private static final String METHOD_TEMPLATE = "\npublic %s %s(%s) {\n" +
            "return (%s) invokeRemote(new MethodContext(\"%s\", \"%s\", %s, %s));}\n";

    private static final String VOID_METHOD_TEMPLATE = "\npublic %s %s(%s) {return;}\n";

    private static final String STUB_NAME_SUFFIX = "$tomato$stub$";

    /**
     * create client stub instance by raw string
     * @param msgSender net
     * @param serviceInterface api interface
     * @param <T> api interface class
     * @return stub instance
     */
    @Override
    @SuppressWarnings("unchecked")
    public <T> T createStub(MsgSender msgSender, Class<T> serviceInterface) {
        String classSimpleName = createSimpleClassName(serviceInterface);
        String classFileName = classSimpleName + ".java";
        String classFullName = STUB_PACKAGE + "." + classSimpleName;
        String stubSourceCode = createStubSourceCode(classSimpleName, serviceInterface);
        JavaStringCompiler compiler = new JavaStringCompiler();
        try {
            Class<?> stubClass = compiler.loadClass(
                    classFullName, compiler.compile(classFileName, stubSourceCode));
            T stubInstance = (T) stubClass.newInstance();
            ((AbstractStub) stubInstance).setMsgSender(msgSender);
            return stubInstance;
        } catch (IOException | ClassNotFoundException |
                IllegalAccessException | InstantiationException e) {
            e.printStackTrace();
        }
        return null;
    }

    private String createStubSourceCode(String className, Class<?> serviceInterface) {
        String interfaceName = createInterfaceName(serviceInterface);
        StringBuilder methodBuilder = new StringBuilder();
        Stream.of(serviceInterface.getMethods())
                .map(method -> createMethodTemplate(interfaceName, method))
                .forEach(methodBuilder::append);
        return String.format(STUB_TEMPLATE,
                className,
                interfaceName,
                methodBuilder.toString());
    }

    private String createSimpleClassName(Class<?> serviceInterface) {
        return serviceInterface.getSimpleName() + STUB_NAME_SUFFIX;
    }

    private String createInterfaceName(Class<?> serviceInterface) {
        return serviceInterface.getName();
    }

    private String createMethodTemplate(String interfaceName, Method method) {
        String methodName = createMethodName(method);
        String returnType = createReturnType(method);
        return void.class.equals(method.getReturnType())
                ? String.format(
                        VOID_METHOD_TEMPLATE,
                        createReturnType(method),
                        methodName,
                        createArgs(method))
                : String.format(
                        METHOD_TEMPLATE,
                        returnType,
                        methodName,
                        createArgs(method),
                        returnType,
                        interfaceName,
                        methodName,
                        returnType + ".class",
                        createArgsWithOutType(method));
    }

    private String createReturnType(Method method) {
        return method.getReturnType().getName();
    }

    private String createMethodName(Method method) {
        return method.getName();
    }

    private String createArgs(Method method) {
        Class<?>[] parameterTypes = method.getParameterTypes();
        if (parameterTypes.length == 0) {
            return "";
        }
        StringBuilder argsBuilder = new StringBuilder();
        for (int i = 0; i < parameterTypes.length; ++i) {
            argsBuilder.append(String.format("%s var%d%c",
                    parameterTypes[i].getName(), i, i == parameterTypes.length - 1 ? ' ' : ','));
        }
        return argsBuilder.toString();
    }

    private String createArgsWithOutType(Method method) {
        int parameterCount = method.getParameterCount();
        if (parameterCount == 0) {
            return "null";
        }
        StringBuilder argsBuilder = new StringBuilder();
        for (int i = 0; i < parameterCount; ++i) {
            argsBuilder.append(
                    String.format("var%d%c", i, i == parameterCount - 1 ? ' ': ','));
        }
        return argsBuilder.toString();
    }
}
