# FBFI

FBFI (Fault-tolerance Bottleneck driven Fault Injection) is a fault injection testing approach to the effective and efficient validation of redundant components deployed in a cloud system. It uses the concept of **fault-tolerance bottleneck** to repeatedly generate fault injection configurations. FBFI does not particularly rely on the system’s complete business structure, but requires that the successful execution paths of the system can be traced.

### Usage

We currently provide a command-line utility that implements the main algorithm of FBFI:

```bash
java -jar Experiment.jar subject=SimulateSubject algorithm=FBFI 
```

To start FBFI, the tester should provide an initial succesful execution path of the system:

```
Please input a successful execution path (e.g. A_1-B_1-C_1):
A_1-B_1-C_1
```

Here, each *execution path* should be encoded as `x_1-x_2-...-x_n`, where `x_i` is a string that indicates the name of a particular business node of the *i*-th service.

FBFI will then update the business structure, and generate its fault-tolerance bottlenecks. The tester should use the given bottleneck to implement concrete fault injections in the testing environment (In the following example, the fault Crash(A_1)&&Crash(B_1) should be injected) and record the results. If an alternative execution path is obtained, the tester should  input this new path to FBFI. FBFI will then update the structure, and calculate new bottlenecks. Otherwise, if this bottleneck can break the system, the tester should input "-" to  FBFI. In any case, the tester should restore the injected config later.

```
Please inject config: [A_1, B_1]
Please input the newly tracked execution path (if the system works correctly), or "-" (if the system fails in all cases):
A_2-B_2-C_1
Path: [A_2, B_2, C_1]
Please Restore Config: [A_1, B_1]
```
After several rounds, if all bottlenecks can break the system, it means that the FBFI has detected  the complete business structure. At this moment, FBFI will output the complete  structure, its all fault-tolerance bottlenecks, and other related information.

```
~ ~ ~ ~ ~ ~ ~ ~ ~ ~ Graph ~ ~ ~ ~ ~ ~ ~ ~ ~ ~
[ Layer 1: A ]
LayerSize: 2
LayerNodes: [A_1, A_2]

NodeID: A_1
Layer: 1
SubNodes: B_2 B_1 
PreNodes: start 

NodeID: A_2
Layer: 1
SubNodes: B_2 B_1 B_3 
PreNodes: start 

[ Layer 2: B ]
LayerSize: 3
LayerNodes: [B_2, B_1, B_3]

NodeID: B_2
Layer: 2
SubNodes: C_1 
PreNodes: A_1 A_2 

NodeID: B_1
Layer: 2
SubNodes: C_1 C_2 
PreNodes: A_1 A_2 

NodeID: B_3
Layer: 2
SubNodes: C_1 C_2 
PreNodes: A_2 

[ Layer 3: C ]
LayerSize: 2
LayerNodes: [C_1, C_2]

NodeID: C_1
Layer: 3
SubNodes: end 
PreNodes: B_2 B_1 B_3 

NodeID: C_2
Layer: 3
SubNodes: end 
PreNodes: B_1 B_3 
~ ~ ~ ~ ~ ~ ~ ~ ~ ~ ~ ~ ~ ~ ~ ~ ~ ~ ~ ~

Bottlenecks: 
[[A_1, A_2], [B_2, B_1, B_3], [C_1, C_2], [C_1, B_1, B_3], [C_1, B_1, A_2], [B_2, B_1, A_2]]
bottleneckNumber = 6

Defects: 
[]
defectNumber = 0
...
```
The experimental data will be recorded in the file `outputs_FBFI.txt`
