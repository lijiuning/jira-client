package com.ljn;

public class SysPath {
    public String jarPath()
    {
        String path = this.getClass().getProtectionDomain().getCodeSource().getLocation().getPath();
        if(path.toUpperCase().indexOf(".JAR")!=-1)
        {
            try
            {
                String StrPath=path.substring(0, path.toUpperCase().indexOf(".jar".toUpperCase()));
                path=StrPath.substring(0,StrPath.lastIndexOf("/")+1);
            }
            catch(Exception e)
            {
                return "";
            }
        }
        return path;
    }


}
