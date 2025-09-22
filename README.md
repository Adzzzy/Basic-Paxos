# Basic Paxos
Paxos is a group of protocols for reaching consensus among processors (or computers) in a distributed system. 
Due to the unreliable nature of networks and computer hardware, Paxos was created with the aim to provide fault-tolerance and consistency throughout this decision-making process.

Basic Paxos is the simplest of the Paxos algorithms and includes several mechanisms to encourage liveness, (i.e. progress continues to be made), and safety, (a guarantee of consistency in agreeing upon a value), in the presence of faults.

## Overview
To see a detailed overview of the Basic Paxos implementation for this project, refer to the "Overview.txt" file.

## Usage

### Running as a Docker container
The easiest way to run this project across a wide variety of different environments is to run it as a Docker container using the image I've created for it.

**Get Docker Desktop**
- Head to https://www.docker.com/products/docker-desktop/ and click on _Download Docker Desktop_, making sure to choose the right download for your device.
- Open Docker Desktop and make sure it's running. (The bottom left corner of the Docker Desktop window indicates the status of the Docker Engine).

**Run the image as a container**
- Open up any terminal or shell. On Windows, Mac, or Linux simply search "Terminal" and open it up. You could also search for "Command Prompt" or "PowerShell" instead on Windows.
- Paste the following into the terminal and press enter: `docker run ghcr.io/adzzzy/basic-paxos`
    - Note: For a specific version of the image, just add `:<VERSION>` onto the end of the image name. </br> Available versions can be seen here: https://github.com/Adzzzy/Basic-Paxos/pkgs/container/basic-paxos
- After a few moments for downloading dependencies and the set-up needed to run on your device, the program will start and you're ready to play!

---------------------------------------------------------------------------------------------------------------------------------------

### Running using an IDE
An Integrated Development Environment for Java, such as Eclipse or IntelliJ, can be used to run the project with minimal set-up.

**Download an IDE**
- The Eclipse IDE can be downloaded here: https://www.eclipse.org/downloads/packages/installer
  - During the installation process, when asked to choose a package, select "Eclipse IDE for Java Developers".
- Alternatively, download IntelliJ here: https://www.jetbrains.com/idea/download
  - Note: IntelliJ requires the Java Development Kit (JDK) in order to compile and run code. For information on how to set that up, see "Manual Set-up" further below.

**Create a new project**
- Open up the IDE and click on the option to create a new project. Choose Java as the project type if it isn't already done so by default.
- Change the project location to the folder where you've stored this repository, or leave it as the default location and drag the files into the project afterwards.

**Run the project**
- Click on the Client.java file from the project files on the left-hand side, then press the green run button on the bar near the top of the project window.

---------------------------------------------------------------------------------------------------------------------------------------

### Manual Set-up
If you'd rather set up your environment manually to compile and run Java code, you do so with the following steps:

**Install the Java Development Kit**
- To download the JDK head to the Oracle webiste and choose the right download for your operating system: https://www.oracle.com/java/technologies/downloads
- Alternatively you can download it via the command line:
  - On Linux download the package with the package manager of your Linux distribution. E.g. on Ubuntu using apt: `sudo apt update && sudo apt install default-jdk`
  - On Mac first install homebrew with `/bin/bash -c "$(curl -fsSL https://raw.githubusercontent.com/Homebrew/install/HEAD/install.sh)"`, then install JDK with `brew update && brew install openjdk`
  - On Windows first download the MSI installer with `curl https://download.oracle.com/java/25/latest/jdk-25_windows-x64_bin.msi -o jdk-25_windows-x64_bin.msi`, then install with `start jdk-25_windows-x64_bin.msi` (replace "25" with the desired version number).

**Configure Environment Variables**

- There are two environment variables associated with the JDK:
  - The **PATH variable**, which needs to be configured with the path to the executables and binaries used by JDK, such as Java and Javac. This is often added automatically when using an installer.
  - The **JAVA_HOME variable**, which contains the path of the entire installation of the Java version. This is optional and it helps some programs find the Java installation files. It typically isn't added automatically.

- The locations for the installation can vary by OS, but is typically found in the following places:
  - On Linux in `/usr/lib/jvm/java-<version>`
  - On Mac in `/Library/Java/JavaVirtualMachines/jdk-<version>.jdk/Contents/Home/`
  - On Windows in `C:\Program Files\Java\jdk-<version>`

- This path can then be set to the **JAVA_HOME** environment variable:
  - On Linux inside the current user's home directory, open the shell configuration file (`~/.bashrc`, `~/.bash_profile` or `~/.profile`), add the line `export JAVA_HOME=/usr/lib/jvm/java-<version>`, then run `source ~/.bashrc` (using the name of your shell configuration file).
  - On Mac inside the current user's home directory, open the shell configuration file (`~/.zshrc`, `~/.bashrc`, or `~/.bash_profile`), add the line `export JAVA_HOME=$(/usr/libexec/java_home)`, then run `source ~/.zshrc` (using the name of your shell configuration file).
  - On Windows search "View advanced system settings", click "Environment Variables" and then under system variables press "New". Put "JAVA_HOME" for the name, and for the value put `C:\Program Files\Java\jdk-<version>`, or the path to your Java installation.

- Next, if your **PATH** environment variable didn't update automatically upon installation, it's recommended you do that. It will allow use of commands without typing the entire path each time.
- The Java executable is usually found within a folder called _"bin"_ somewhere inside your Java installation's folder i.e. JAVA_HOME.
  - On Linux the path is usually `/usr/lib/jvm/java-<version>/bin`. Add it using the same method as for JAVA_HOME, but call it PATH and put `:$PATH` on the end. E.g. `export PATH=/usr/lib/jvm/java-<version>/bin:$PATH`
  - On Mac the path looks like: `/Library/Java/JavaVirtualMachines/jdk-<version>.jdk/Contents/Home/bin`. You can open the shell configuration file again, this time adding `export PATH=/Library/Java/JavaVirtualMachines/jdk-<version>/Contents/Home/bin:$PATH`, making sure `:$PATH` is included on the end.
  - On Windows the path will be something like this: `C:\Program Files\Java\jdk-<version>\bin`. To add it to the Path variable, go to Environment Variables again, this time clicking on "Path" in the system variables list. Click "Edit", then "New", and paste the full path inside, including the bin folder.
  - Note: If you already set up JAVA_HOME earlier, you can use that variable in place of the full path name E.g. `$JAVA_HOME/bin` in Linux and Mac, or `%JAVA_HOME/bin` in Windows.

**Compile the Java files**
- To compile the project, since everything is in the same directory, you can simply compile the client, and everything else will be found and compiled automatically: `javac Client.java`

**Run the compiled Executable**
- Once compiled, simply run the executable by typing: `java Client`
