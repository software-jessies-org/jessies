package e.tools;

import e.io.*;
import java.io.*;
import java.lang.reflect.*;
import java.net.*;
import java.util.*;

/**
 * Produces C++ for implementing native methods, as javah(1) produces C.
 */
public class JavaHpp {
    private List<URL> classpath = new ArrayList<URL>();
    
    public JavaHpp(String[] arguments) throws Exception {
        for (int i = 0; i < arguments.length; ++i) {
            String argument = arguments[i];
            if (argument.equals("-classpath")) {
                URL url = new File(arguments[++i]).toURI().toURL();
                classpath.add(url);
            } else {
                String className = argument;
                IndentedSourceWriter out = new IndentedSourceWriter(System.out);
                processClass(out, className);
            }
        }
    }
    
    public void processClass(IndentedSourceWriter out, String className) throws Exception {
        String includeGuardName = className.replace('.', '_') + "_included";
        out.println("#ifndef " + includeGuardName);
        out.println("#define " + includeGuardName);
        
        out.println("#include <jni.h>");
        out.println("#include <JniField.h>");
        out.println("#include <stdexcept>");
        
        Class klass = Class.forName(className, false, new URLClassLoader(classpath.toArray(new URL[0])));
        List<Method> nativeMethods = extractNativeMethods(klass.getDeclaredMethods());
        List<Field> instanceFields = extractInstanceFields(klass.getDeclaredFields());
        
        String proxyClassName = className.replace('.', '_');
        out.println("class " + proxyClassName + " {");
        out.println("private:");
        out.println("JNIEnv* m_env;");
        out.println("jobject m_instance;");
        for (Field field : instanceFields) {
            out.println("JniField<" + jniTypeNameFor(field.getType()) + "> " + field.getName() + ";");
        }
        emit_newStringUtf8(out);
        
        out.println("public:");
        out.println(proxyClassName + "(JNIEnv* env, jobject instance)");
        out.println(": m_env(env)");
        out.println(", m_instance(instance)");
        for (Field field : instanceFields) {
            out.println(", " + field.getName() + "(env, instance, \"" + field.getName() + "\", \"" + encodedTypeNameFor(field.getType()) + "\")");
        }
        out.println("{");
        out.println("}");
        for (Method method : nativeMethods) {
            StringBuilder proxyMethodArguments = new StringBuilder();
            for (Class parameterType : method.getParameterTypes()) {
                if (proxyMethodArguments.length() > 0) {
                    proxyMethodArguments.append(", ");
                }
                proxyMethodArguments.append(jniTypeNameFor(parameterType));
            }
            out.println(jniTypeNameFor(method.getReturnType()) + " " + method.getName() + "(" + proxyMethodArguments + ");");
        }
        out.println("};");
        
        emit_translateToJavaException(out);
        
        // Output JNI global C functions.
        for (Method method : nativeMethods) {
            String jniMangledClassName = klass.getCanonicalName().replace('.', '_') + "_";
            String jniMangledName = "Java_" + jniMangledClassName + method.getName();
            String jniReturnType = jniTypeNameFor(method.getReturnType());
            String parameters = "JNIEnv* env, jobject instance";
            String arguments = "";
            int argCount = 0;
            for (Class parameterType : method.getParameterTypes()) {
                parameters += ", " + jniTypeNameFor(parameterType) + " a" + argCount;
                if (arguments.length() > 0) {
                    arguments += ", ";
                }
                arguments += "a" + argCount;
                ++argCount;
            }
            
            out.println("extern \"C\" JNIEXPORT " + jniReturnType + " JNICALL " + jniMangledName + "(" + parameters + ") {");
            out.println("try {");
            out.println(proxyClassName + " proxy(env, instance);");
            String proxyMethodCall = "proxy." + method.getName() + "(" + arguments + ")";
            if (method.getReturnType() == Void.TYPE) {
                out.println(proxyMethodCall + ";");
            } else {
                out.println("return " + proxyMethodCall + ";");
            }
            out.println("} catch (std::exception& ex) {");
            out.println("translateToJavaException(env, \"" + chooseExceptionClassName(method) + "\", ex);");
            if (method.getReturnType() != Void.TYPE) {
                out.println("return " + jniTypeNameFor(method.getReturnType()) + "();");
            }
            out.println("}");
            out.println("}");
        }
        
        out.println("#endif // " + includeGuardName);
    }
    
    private String chooseExceptionClassName(Method method) {
        Class[] exceptionTypes = method.getExceptionTypes();
        if (exceptionTypes.length == 0) {
            return "java/lang/RuntimeException";
        } else if (exceptionTypes.length == 1) {
            return slashStyleClassName(exceptionTypes[0]);
        }
        throw new RuntimeException("methods such as '" + method + "' with multiple exception types are not supported; please choose a single exception type or enhance JavaHpp to use an annotation to choose a wrapper exception for C++ exceptions");
    }
    
    private List<Method> extractNativeMethods(Method[] methods) {
        List<Method> nativeMethods = new ArrayList<Method>();
        for (Method method : methods) {
            if ((method.getModifiers() & Modifier.NATIVE) != 0) {
                nativeMethods.add(method);
            }
        }
        return nativeMethods;
    }
    
    private List<Field> extractInstanceFields(Field[] fields) {
        List<Field> instanceFields = new ArrayList<Field>();
        for (Field field : fields) {
            if ((field.getModifiers() & Modifier.STATIC) != 0) {
                // JniField.h doesn't support static fields, though there's no
                // reason why we couldn't write JniStaticField if we needed it.
                continue;
            }
            instanceFields.add(field);
        }
        return instanceFields;
    }
    
    private void emit_translateToJavaException(IndentedSourceWriter out) {
        out.println("static void translateToJavaException(JNIEnv* env, const char* exceptionClassName, const std::exception& ex) {");
        out.println("    jclass exceptionClass = env->FindClass(exceptionClassName);");
        out.println("    if (exceptionClass) {");
        out.println("        env->ThrowNew(exceptionClass, ex.what());");
        out.println("    }");
        out.println("}");
    }
    
    private void emit_newStringUtf8(IndentedSourceWriter out) {
        out.println("jstring newStringUtf8(const std::string& s) {");
        out.println("    return m_env->NewStringUTF(s.c_str());");
        out.println("}");
    }
    
    public static String jniTypeNameFor(Class type) {
        if (type.isPrimitive()) {
            if (type == void.class) {
                return "void";
            }
            return "j" + type.toString();
        } else if (type.isArray()) {
            // Note that we shouldn't recurse here because there are no JNI
            // types for arrays of arrays. If we ever write our own C++ wrapper
            // for JNI arrays, this might have to change. (We don't need to
            // worry about void[] either, because there's no such thing.)
            Class componentType = type.getComponentType();
            if (componentType.isPrimitive()) {
                return "j" + componentType.toString() + "Array";
            }
            return "jobjectArray";
        } else if (type == String.class) {
            return "jstring";
        } else {
            return "jobject";
        }
    }
    
    public static String encodedTypeNameFor(Class type) {
        if (type.isArray()) {
            throw new RuntimeException("FIXME: array types unimplemented");
        }
        if (type == boolean.class) {
            return "Z";
        } else if (type == byte.class) {
            return "B";
        } else if (type == char.class) {
            return "C";
        } else if (type == double.class) {
            return "D";
        } else if (type == float.class) {
            return "F";
        } else if (type == int.class) {
            return "I";
        } else if (type == long.class) {
            return "J";
        } else if (type == short.class) {
            return "S";
        } else if (type == void.class) {
            return "V";
        } else {
            return "L" + slashStyleClassName(type) + ";";
        }
    }
    
    /**
     * Returns the name used by JNI to refer to a class, which isn't directly
     * available otherwise.
     */
    public static String slashStyleClassName(Class c) {
        String name = c.getCanonicalName().replace('.', '/');
        if (c.getEnclosingClass() != null) {
            // Replace the last '/' with a '$'. This isn't correct in general,
            // but it copes with the simplest inner classes.
            name = name.replaceFirst("^(.*)/([^/]+)$", "$1\\$$2");
        }
        return name;
    }
    
    public static void main(String[] arguments) throws Exception {
        JavaHpp javaHpp = new JavaHpp(arguments);
    }
}
