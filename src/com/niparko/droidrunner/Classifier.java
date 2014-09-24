package com.niparko.droidrunner;

class Classifier {

  public static double classify(Object[] i)
    throws Exception {

    double p = Double.NaN;
    p = Classifier.N2e2395250(i);
    return p;
  }
  static double N2e2395250(Object []i) {
    double p = Double.NaN;
    if (i[27] == null) {
      p = 0;
    } else if (((Double) i[27]).doubleValue() <= 3.213888) {
      p = 0;
    } else if (((Double) i[27]).doubleValue() > 3.213888) {
    p = Classifier.N2214c7cd1(i);
    } 
    return p;
  }
  static double N2214c7cd1(Object []i) {
    double p = Double.NaN;
    if (i[0] == null) {
      p = 1;
    } else if (((Double) i[0]).doubleValue() <= 533.05716) {
    p = Classifier.N24975362(i);
    } else if (((Double) i[0]).doubleValue() > 533.05716) {
      p = 2;
    } 
    return p;
  }
  static double N24975362(Object []i) {
    double p = Double.NaN;
    if (i[8] == null) {
      p = 1;
    } else if (((Double) i[8]).doubleValue() <= 31.552012) {
      p = 1;
    } else if (((Double) i[8]).doubleValue() > 31.552012) {
    p = Classifier.N24ae2d663(i);
    } 
    return p;
  }
  static double N24ae2d663(Object []i) {
    double p = Double.NaN;
    if (i[5] == null) {
      p = 1;
    } else if (((Double) i[5]).doubleValue() <= 22.091831) {
      p = 1;
    } else if (((Double) i[5]).doubleValue() > 22.091831) {
      p = 2;
    } 
    return p;
  }
}

