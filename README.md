# simpleCluster

[<img alt="Travis CI Build Status" src="http://img.shields.io/travis/thomasWeise/simpleCluster/master.svg" height="20"/>](http://travis-ci.org/thomasWeise/simpleCluster/)

## 1. Introduction

This is a very trivial job scheduling engine for small clusters.
Here, using some sophisticated system is just too complicated.
Instead, the idea is that all worker computers mount a shared directory.
Inside this shared directory, we put the binaries and data of all jobs to be executed.
There also is a job queue file to which jobs can get appended and these are then executed by the workers.
No rights management, no ssh, no nothing spectacular.
Just a simple jar archive for both submitting jobs and launching worker threads.
Each job is processed by a single worker thread.
Don't expect any high performance or great scalability.
I don't care about that.
I just want a simple distributed job executor that only needs a minimum software installation (i.e., Java). 

## 2. Usage

The concept of our simple scheduler is building on shared directories.
All worker computers as well as the job-submitting computers must mount the shared directory of the cluster at exactly the same path the `simpleCluster.jar` must be executed in this shared directory.

### 2.1. Job Submission

`java -jar simpleCluster.jar submit cmd=COMMAND dir=DIRECTORY [times=TIMES]`

Enter the command `COMMAND` to be executed in directory `DIRECTORY` into the job queue.
If `times` is specified, the command is added `TIMES` times.
Generally, the directory `DIRECTORY` must exist on all worker computers in the same path.
Usually, this would be a shared, mounted directory.
We will then execute the shell `sh` in that directory and write `COMMAND` to its stdin.
The parameter `times` allows you to submit the same command a number of times.

### 2.2. Job Execution

`java -jar simpleCluster.jar run [cores=nCORES]`

Start the worker threads that pick up the jobs and execute them one after the other.
Via `cores`, you can define the number of workers to launch.
If `cores` is not specified, the number of workers will be equal to the number of processor cores.

### 2.3. Shared Directories under Ubuntu

To create a permanently shared directory under Linux, proceed as follows.

1. Create the shared directory on the main file server computer, let's call the share `cluster`.
2. On every single of the working computers and on the computer from which you want to submit jobs, proceed as follows:
    a. `sudo mkdir -p /cluster` (create the local cluster directory)
    b. `sudo nano /etc/fstab` to edit the file system list
    c. add the line `//SERVER_IP/cluster /cluster/ cifs guest,username=USER,password=PASSWORD,iocharset=utf8,file_mode=0777,dir_mode=0777,noperm 0 0`, where `SERVER_IP` be the IP-address of the file server, and `USER` and `PASSWORD` be the username and password.
    d. save and exit `nano`
    e. do `sudo mount -a`

You now can access the same shared directory, `/cluster`, from your job submission PC and from all worker PCs.
Now you should copy the `simpleCluster.jar` there.
Start it in the Job Execution option on each worker.
You can now create a sub-directory for each work job under `/cluster` and put, say, shell scripts and data in there.
From the job submission PC, you can then enqueue these scripts using the Job Submission mode. 

## 3. Licensing

This software is licensed under the GNU General Public License 3.0.

## 4. Contact

If you have any questions or suggestions, please contact
[Prof. Dr. Thomas Weise](http://iao.hfuu.edu.cn/team/director) of the
[Institute of Applied Optimization](http://iao.hfuu.edu.cn/) at
[Hefei University](http://www.hfuu.edu.cn) in
Hefei, Anhui, China via
email to [tweise@hfuu.edu.cn](mailto:tweise@hfuu.edu.cn) with CC to [tweise@ustc.edu.cn](mailto:tweise@ustc.edu.cn).
