# VCS—Assessing code contributions in version control systems—TIC3901 Industrial Practice

## Description
This project is a Git log analyzer. It parses the Git log to extract commit information, including the commit ID, author, and lines added or removed. This information is used to generate statistics about each author's contributions to the project, such as the number of lines added or removed and the number of commits made. The project also uses the Git blame command to determine the current owner of each line in the project's files. This information is used to generate a report showing the ownership of each file in the project.

## Installation
This project is built with Java and Maven. To install and set up the project, follow these steps:

1. Clone the repository: `git clone https://github.com/amber-le/VCS.git`
2. Navigate to the project directory: `cd VCS`
3. Build the project: `mvn clean install`

## Guide to Use the Program


## Guide to Use the Program
Follow these steps to utilize the VCS program effectively:

1. **Clone a Git Repository**: Ensure you have a Git repository available locally. If you don't have one, clone a repository from a source of your choice.

2. **Run Main.java**: Locate and execute the `Main.java` file within the project.

3. **Input the Repository Full Path**: The program will prompt you to enter the full path of the repository you wish to analyze. Provide the path as requested and press Enter to proceed.

4. **Wait for Execution and Obtain Output**: Allow the program to complete its execution. Once the process is finished, you will find two output files generated: a `.csv` file and an `.html` file. These files contain the analyzed data and insights derived from the Git repository.

Enjoy analyzing your project's version control history with VCS!

---

*Note: Ensure you have Java and Maven installed on your system before proceeding with the installation steps.*
