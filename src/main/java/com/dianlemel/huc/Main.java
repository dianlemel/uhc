package com.dianlemel.huc;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class Main {

    public static void main(String[] args) throws Exception {
        List<String> a = new ArrayList<>();
        a.add("1");
        a.add("2");
        a.add("3");
        a.forEach(System.out::println);
        System.out.println("========================");
        System.out.println(a.remove("2"));
        a.forEach(System.out::println);
        System.out.println("========================");
        System.out.println(a.remove("4"));
        a.forEach(System.out::println);
    }

}
