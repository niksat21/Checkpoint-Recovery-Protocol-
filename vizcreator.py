import csv
from seqdiag import parser, builder, drawer

files = []
#for i in xrange(1, 7):
#	files.append("/Users/vads/NetBeansProjects/AOS Assignment3/src/client" + str(i) + "_FILESTORAGE/viz.log")

files = ["/media/ram/pendrive/log.txt"]

allCSVRows = []
for file in files:
	with open(file, ) as csvfile:
		reader = csv.DictReader(csvfile, fieldnames=['time', 'sentts', 'receivedts', 'sentnode', 'receivednode', 'messagetype'])
		# print "total rows in %s : %d" %(file, len(list(reader)))
		for row in reader:
			#print "%d %d %d %s %s %s" %(int(row['time']), row['sentts'], row['receivedts'], row['sentnode'], row['receivednode'], row['messagetype'])
			row['time'] = int(row['time'])
			row['sentts'] = int(row['sentts'])
			row['receivedts'] = int(row['receivedts'])
			allCSVRows.append(row)

# Order by time, sentts and receivedts
def compare(item1, item2):
	if item1['time'] == item2['time']:
		if item1['receivedts'] == item2['receivedts']:
			return item1['receivedts'] - item2['receivedts']
		else:
			return item1['sentts'] - item2['sentts']
	else:
		return item1['time'] - item2['time']

allCSVRows = sorted(allCSVRows, cmp=compare)

#for row in allCSVRows:
#	print "%d %d %d %s %s %s" %(row['time'], row['sentts'], row['receivedts'], row['sentnode'], row['receivednode'], row['messagetype'])
  #client1; client2; client3; client4; client5; client6; client7; server1; server2; server3;

diagram_definition = u"""
seqdiag {

"""
#
for row in allCSVRows:
	diagram_definition = diagram_definition + "  " + row['sentnode'] + " -> " + row['receivednode'] + "[label='[" + str(row['sentts']) + "] " + row['messagetype'] + "', "
	if row['sentnode'] >= row['receivednode']:
		diagram_definition = diagram_definition + "leftnote='[" + str(row['receivedts']) + "]'];\n"
	else:
		diagram_definition = diagram_definition + "rightnote='[" + str(row['receivedts']) + "]'];\n"
	#print "%d %d %d %s %s %s" %(row['time'], row['sentts'], row['receivedts'], row['sentnode'], row['receivednode'], row['messagetype'])

diagram_definition = diagram_definition + "}"
print diagram_definition

print "total rows :%d" % (len(allCSVRows))


tree = parser.parse_string(diagram_definition)
print "parse complete"
diagram = builder.ScreenNodeBuilder.build(tree)
print "build complete"
draw = drawer.DiagramDraw('PNG', diagram, filename="diagram.png")
print "draw init complete"
draw.draw()
print "draw complete"
draw.save()

