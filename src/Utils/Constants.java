package Utils;

/**
 * Created by toni on 28/03/2017.
 */
public class Constants {

    public static String selfParameterIdentifier = "self";
    public static String classDescriptorIdentifier = "descriptor";
    public static String classDescriptorClassIdentifier = "ClassDescriptor";
    public static String standardLibraryIdentifier = "stdlib";

    public static String descriptorName(String className) {
        return className + "_" + Constants.classDescriptorIdentifier;
    }
}
