package base.operators.operator.scripting.python;

import java.io.File;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class PythonBinaryComparator implements Comparator<String> {
    private static Pattern patternPythonBinary;

    public PythonBinaryComparator() {
    }

    private int compareBasedOnVersion(String fullName1, String fileName1, String fullName2, String fileName2) {
        PythonBinaryComparator.Version version1 = new PythonBinaryComparator.Version(fileName1);
        PythonBinaryComparator.Version version2 = new PythonBinaryComparator.Version(fileName2);
        if (version1.major == version2.major) {
            if (version1.minor == version2.minor) {
                if (version1.suffix.length() - version2.suffix.length() != 0) {
                    return version1.suffix.length() - version2.suffix.length();
                } else if (version1.isExeExtension && !version2.isExeExtension) {
                    return 1;
                } else {
                    return version2.isExeExtension && !version1.isExeExtension ? -1 : fullName1.compareTo(fullName2);
                }
            } else {
                return version1.minor < version2.minor ? 1 : -1;
            }
        } else {
            return version1.major < version2.major ? 1 : -1;
        }
    }

    @Override
    public int compare(String o1, String o2) {
        if (o1.equals(o2)) {
            return 0;
        } else {
            String name1 = Paths.get(o1).getFileName().toString();
            String name2 = Paths.get(o2).getFileName().toString();
            if (name1.equals(name2)) {
                return o1.compareTo(o2);
            } else {
                Matcher matcherPythonBinary1 = patternPythonBinary.matcher(o1);
                Matcher matcherPythonBinary2 = patternPythonBinary.matcher(o2);
                if (!matcherPythonBinary1.find()) {
                    return !matcherPythonBinary2.find() ? name1.compareTo(name2) : 1;
                } else {
                    return !matcherPythonBinary2.find() ? -1 : this.compareBasedOnVersion(o1, name1, o2, name2);
                }
            }
        }
    }

    public static List<String> sortByDirectory(List<String> list) {
        List<String> result = new ArrayList(list.size());
        Set<String> visitedDirectories = new HashSet();
        Iterator var3 = list.iterator();

        while(true) {
            String parent;
            do {
                if (!var3.hasNext()) {
                    return result;
                }

                String s = (String)var3.next();
                parent = Paths.get(s).getParent().toString();
            } while(visitedDirectories.contains(parent));

            Iterator var6 = list.iterator();

            while(var6.hasNext()) {
                String ss = (String)var6.next();
                String pp = Paths.get(ss).getParent().toString();
                if (parent.equals(pp)) {
                    result.add(ss);
                    visitedDirectories.add(pp);
                }
            }
        }
    }

    static {
        patternPythonBinary = Pattern.compile(String.format("\\%spython[^\\%s]*$", File.separator, File.separator));
    }

    private static class Version {
        private static Pattern patternMajorVersion;
        private static Pattern patternMajorMinorVersion;
        private static Pattern patternSuffix;
        private static Pattern patternExeSuffix;
        private final int major;
        private final int minor;
        private final String suffix;
        private final boolean isExeExtension;

        public Version(String fileName) {
            Matcher matcherMajorMinor = patternMajorMinorVersion.matcher(fileName);
            Matcher matcherSuffix;
            if (matcherMajorMinor.find()) {
                this.major = Integer.parseInt(matcherMajorMinor.group("major"));
                this.minor = Integer.parseInt(matcherMajorMinor.group("minor"));
            } else {
                matcherSuffix = patternMajorVersion.matcher(fileName);
                if (matcherSuffix.find()) {
                    this.major = Integer.parseInt(matcherSuffix.group("major"));
                } else {
                    this.major = 2147483647;
                }

                this.minor = 2147483647;
            }

            matcherSuffix = patternSuffix.matcher(fileName);
            if (matcherSuffix.find()) {
                this.suffix = matcherSuffix.group("suffix");
            } else {
                this.suffix = "";
            }

            this.isExeExtension = patternExeSuffix.matcher(fileName).find();
        }

        static {
            patternMajorVersion = Pattern.compile(String.format("python(?<major>[0-9])[^\\%s]*$", File.separator));
            patternMajorMinorVersion = Pattern.compile(String.format("python(?<major>[0-9])\\.?(?<minor>[0-9])[^\\%s]*$", File.separator));
            patternSuffix = Pattern.compile(String.format("python[0-9]{0,2}(?<suffix>[a-z])[^\\%s]*$", File.separator));
            patternExeSuffix = Pattern.compile(String.format("python[0-9]{0,2}[^\\%s]*\\.exe$", File.separator));
        }
    }
}

