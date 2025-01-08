package com.ll.framework.ioc;

import com.ll.framework.ioc.annotations.Component;
import com.ll.framework.ioc.annotations.Service;
import javassist.tools.reflect.CannotCreateException;
import org.reflections.Reflections;
import org.reflections.scanners.Scanners;

import java.lang.reflect.Constructor;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Stream;

public class ApplicationContext {
    private final Map<String, Object> context = new HashMap<>();
    private final String basePackage;
    private final String baseUri = "./src/test/java";

    public ApplicationContext(String basePackage) {
        this.basePackage = basePackage;
    }

    public void init() {
    }

    public <T> T genBean(String beanName) {
        if (context.get(beanName) != null) { return (T) context.get(beanName); }
        String fullDirName =  beanName.substring(0,1).toUpperCase() + beanName.substring(1);

        Reflections reflectionsAll = new Reflections(basePackage, Scanners.TypesAnnotated); // your.root.package 하위의 모든 패키지 스캔
        Set<Class<?>> annotatedClassesAll = reflectionsAll.getTypesAnnotatedWith(Component.class);

        try {
            List<String> files;
            //TODO beanName 과 일치하는 경로를 동적으로 찾는 부분을 메서드 분리
            files = annotatedClassesAll.stream()
                .map(Class::getName)
                .filter(name -> name.contains(fullDirName))
                .toList();

            //만약 의존 객체를 프로젝트 디렉토리에서 찾을 수 없다면 BaseCreateExcpetion 처리
            if (files.getFirst() == null)  {throw new CannotCreateException("클래스 " + fullDirName + " 를 찾을 수 없습니다"); }

            //해당 이름과 일치하는 첫 번째 클래스 생성
            Class<T> bean = (Class<T>) Class.forName(files.getFirst());
            Constructor<T> constructor = (Constructor<T>) bean.getConstructors()[0];

            Class[] parameters = constructor.getParameterTypes();
            List<T> instancedParameters = new ArrayList<>();

            //재귀적으로 필요한 인자를 동적으로 생성
            for (Class parameter : parameters) {
                String[] parts = parameter.getName().split("\\.");
                String parameterName = parts[parts.length - 1];
                String firstLetter = parameterName.substring(0,1).toLowerCase();
                T instance = genBean(firstLetter + parameterName.substring(1));
                instancedParameters.add(instance);
            }

            T instance = constructor.newInstance(instancedParameters.toArray());
            context.put(beanName, instance);
            return instance;

        } catch (CannotCreateException e){
            //TODO 만약 빈을 생성할 수 없다면 따로 후처리
            return (T) null;
        } catch (Exception e) {
            return (T) null;
        }
    }
}