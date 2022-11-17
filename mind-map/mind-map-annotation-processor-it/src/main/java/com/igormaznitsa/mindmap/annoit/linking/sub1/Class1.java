package com.igormaznitsa.mindmap.annoit.linking.sub1;

import com.igormaznitsa.mindmap.annotations.Direction;
import com.igormaznitsa.mindmap.annotations.MmdFileLink;
import com.igormaznitsa.mindmap.annotations.MmdTopic;

@MmdFileLink(uid = "sub1")
@MmdTopic(direction = Direction.LEFT)
public class Class1 {

  @MmdTopic
  public void method1(@MmdTopic int a, @MmdTopic int b){

  }

  @MmdTopic
  public void method2(@MmdTopic int a, @MmdTopic int b){

  }

  @MmdTopic
  public void method3(@MmdTopic int c){

  }

}