# simpleCluster

[<img alt="Travis CI Build Status" src="http://img.shields.io/travis/thomasWeise/simpleCluster/master.svg" height="20"/>](http://travis-ci.org/thomasWeise/simpleCluster/)

## 1. Introduction

This is a very trivial job scheduling engine for small clusters.
Here, using some [sophisticated systems](https://en.wikipedia.org/wiki/Comparison_of_cluster_software) like [TORQUE](https://en.wikipedia.org/wiki/TORQUE_Resource_Manager) or [SLURM](https://en.wikipedia.org/wiki/Slurm_Workload_Manager) is just too complicated, as they require a lot of installation and configuration effort.
Instead, we aim to develop a very simple system for distributing jobs that does not require any installation (except for Java) and has one single entry point for job submission and execution.
We also do not care about rights management or systems security, as this system is strictly for personal use.

The idea is that all worker computers mount a shared directory under exactly the same path.
You should also mount this directory on your own PC.
Inside this shared directory, we put the binaries and data of all jobs to be executed.
For each job, you could create a directory, put the binary and data and a shell script for executing the job inside.
There also is a job queue file to which jobs can get appended and these are then executed by the workers.
No rights management, no ssh, no nothing spectacular.
Just a simple jar archive for both submitting jobs to the queue and for launching jobs in worker threads.
Each job is processed by a single worker thread.
No management of parallelism except for that is done, i.e., jobs may be programs that can spawn arbitrarily many own threads.
However, you can tag a job as `blocksMachine` meaning that no other job can be executed in parallel on the same machine.

There is no central scheduling.
Instead, the workers will query the job queue file for new tasks.
Via a lock file, it is ensured that only one worker can read the queue file at once.
For each worker PC, at most one idle thread will be querying the central job queue file.
Since there is no central scheduling, we do not need any cluster management software or resource management software.

Don't expect any high performance or great scalability.
I don't care about that.
I just want a simple distributed job executor that only needs a minimum software installation (i.e., Java).
It uses a simple text file and a lock file for queue management, so if you have more than 10 or so worker PCs and more than 1000 or so jobs at once, expect a significant performance decrease.

For any more sophisticated cluster usage, e.g., for one that will work with more nodes or has user management or other fancy features, please check [this list](https://en.wikipedia.org/wiki/Comparison_of_cluster_software).

## 2. Usage

The concept of our simple scheduler is building on shared directories.
All worker computers as well as the job-submitting computers must mount the shared directory of the cluster at exactly the same path the `simpleCluster.jar` must be executed in this shared directory.

### 2.1. Job Submission

`java -jar simpleCluster.jar submit cmd=COMMAND dir=DIRECTORY [times=TIMES] [blocksMachine]`

Enter the command `COMMAND` to be executed in directory `DIRECTORY` into the job queue.
If `times` is specified, the command is added `TIMES` times.
Generally, the directory `DIRECTORY` must exist on all worker computers in the same path.
Usually, this would be a shared, mounted directory.
The job executors will then execute the shell in that directory and write `COMMAND` to its stdin.
The parameter `times` allows you to submit the same command a number of times.
A job tagged with `blocksMachine` will block all job execution on one machine.
It will only begin executing once all the threads on the machine are idle and no thread will begin or query for a new job until the blocking job is completed.
This is intended for jobs that either spawn their own threads or that require lots of memory or do heavy I/O and thus might disturb other jobs running in parallel.

### 2.2. Job Execution

`java -jar simpleCluster.jar run [cores=nCORES] [sh=/path/to/shell]`

On each worker PC, you should launch one instance of the job executor.
It will start the worker threads that pick up the jobs and execute them one after the other.
Via `cores`, you can define the number of workers to launch.
If `cores` is not specified, the number of workers will be equal to the number of processor cores.
If `sh` is specified, it must be the path to the shell receiving the commands.
If `sh` is not specified, we will use the default shell.
For every command received, a new instance of the shell is launched and the command is piped to it.
Once the shell has terminated, the worker thread will query for the next command.

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
