__author__ = 'niksat21'

import sys

OPVsNode = {}

def fetchGlobalState():
    NodeVsFLS = {}
    NodeVsLLR = {}
    NodeVsLLS = {}
    NodeVsNBR = {}

    # get FLS
    tFLS = line[line.index("FLS"):]
    FLS = tFLS[5:tFLS.index("]")].split(", ")
    NodeVsFLS.update({nodeId: FLS})
    # get LLR
    tLLR = line[line.index("LLR"):]
    LLR = tLLR[5:tLLR.index("]")].split(", ")
    NodeVsLLR.update({nodeId: LLR})
    # get LLS
    tLLS = line[line.index("LLS"):]
    LLS = tLLS[5:tLLS.index("]")].split(", ")
    NodeVsLLS.update({nodeId: LLS})
    # get Neighbors
    tNBR = line[line.index("Neighbors"):]
    NBR = tNBR[11:tNBR.index("]")].split(", ")
    NodeVsNBR.update({nodeId: NBR})

    print 'OPVsNode',OPVsNode
    print 'NodeVsFLS', NodeVsFLS
    print 'NodeVsLLR', NodeVsLLR
    print 'NodeVsLLS', NodeVsLLS
    print 'NodeVsNBR', NodeVsNBR


def checkForOrphanMsg(OpId):
    if "R" in OpId:
        print
    elif "C" in OpId:

        print
    else:
        print 'unsupported operation'


with open(sys.argv[1], 'r') as f:
    x = f.readlines()
    currentOp = ""
    nodes = []
    for line in x:
        if "Operation completed in NodeId" in line:
            nodeId = line[line.index("NodeId") + 7:line.index("NodeId") + 8]
            opId = line[line.index("OperationId") + 12:line.index("OperationId") + 15]

            print nodeId, opId, currentOp
            if currentOp == "":
                currentOp = opId
                print nodeId, opId, currentOp
                nodes.append(nodeId)
                OPVsNode.update({opId : nodes})
                fetchGlobalState()

            else:
                currentOp = opId
                print currentOp
                currentOp = ""
                nodes.append(nodeId)
                OPVsNode.update({opId : nodes})
                fetchGlobalState()
