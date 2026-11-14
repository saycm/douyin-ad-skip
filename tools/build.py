from pathlib import Path
import os
import shutil
import subprocess
import zipfile

ROOT = Path(__file__).resolve().parents[1]
SDK = Path(os.environ["ANDROID_HOME"])
ANDROID = SDK / "platforms/android-36/android.jar"
TOOLS = SDK / "build-tools/36.0.0"
BUILD = ROOT / "build"
ENTRY = "com.inkdye.fuckminiappad.hook.Compat41Entry"


def run(*args):
    subprocess.run([str(arg) for arg in args], check=True, cwd=ROOT)


def main():
    for folder in ("classes", "stubs", "dex", "res"):
        directory = BUILD / folder
        directory.mkdir(parents=True, exist_ok=True)
        if folder in ("classes", "stubs"):
            for path in directory.rglob("*.class"):
                path.unlink()
    run(shutil.which("javac"), "-encoding", "UTF-8", "-source", "8", "-target", "8", "-Xlint:-options",
        "-d", BUILD / "stubs", *sorted((ROOT / "stubs").rglob("*.java")))
    run(shutil.which("javac"), "-encoding", "UTF-8", "-source", "8", "-target", "8", "-Xlint:-options",
        "-cp", os.pathsep.join((str(ANDROID), str(BUILD / "stubs"))), "-d", BUILD / "classes",
        *sorted((ROOT / "src/main/java").rglob("*.java")))
    runtime = BUILD / "runtime.jar"
    run(shutil.which("jar"), "cf", runtime, "-C", BUILD / "classes", ".")
    run(SDK / "cmdline-tools/latest/bin/d8.bat", "--release", "--min-api", "29",
        "--lib", ANDROID, "--classpath", BUILD / "stubs", "--output", BUILD / "dex", runtime)
    resource_root = ROOT / "module"
    run(TOOLS / "aapt2.exe", "compile", "--dir", resource_root / "res", "-o", BUILD / "res/compiled.zip")
    linked = BUILD / "linked.apk"
    run(TOOLS / "aapt2.exe", "link", "-o", linked, "-I", ANDROID,
        "--manifest", resource_root / "AndroidManifest.xml", BUILD / "res/compiled.zip")
    with zipfile.ZipFile(linked, "a", compression=zipfile.ZIP_DEFLATED) as apk:
        apk.write(BUILD / "dex/classes.dex", "classes.dex")
        apk.writestr("assets/xposed_init", ENTRY + "\n")
    aligned = BUILD / "aligned.apk"
    run(TOOLS / "zipalign.exe", "-f", "-p", "4", linked, aligned)
    output = BUILD / "FuckMiniappAD_Multi_1.3-unsigned.apk"
    shutil.copyfile(aligned, output)
    print(output)


if __name__ == "__main__":
    main()
