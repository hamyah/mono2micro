package pt.ist.socialsoftware.mono2micro.utils;

public final class Constants {
    
  private Constants(){
  }
  
  public static String CODEBASES_PATH = "src/main/resources/codebases/";
  public static String RESOURCES_PATH = "src/main/resources/";
  public static String PYTHON = PropertiesManager.getProperties().getProperty("python");
  public static final String DEFAULT_REDESIGN_NAME = "Monolith Trace";

  enum Mode {
    READ,
    WRITE,
    READWRITE
  }
}