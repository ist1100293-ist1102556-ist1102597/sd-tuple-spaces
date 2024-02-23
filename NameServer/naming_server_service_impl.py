import sys
sys.path.insert(1, '../Contract/target/generated-sources/protobuf/python')

import NameServer_pb2 as pb2
import NameServer_pb2_grpc as pb2_grpc
import grpc
import logging
from naming_server_state import *;

class NamingServerServiceImpl(pb2_grpc.NameServerServicer):

    def __init__(self):
        self.state = NamingServer()

    def Register(self, request, context):
        self.state.register_server(request.name, ServerEntry(request.qualifier, request.host))
        
        logging.info(f'Register: {request.name} | {request.qualifier if request.qualifier else "No Qualifier"} | {request.host}')
        return pb2.RegisterResponse()
    
    def Lookup(self, request, context):
        if (len(request.qualifier)) == 0:
            hosts=self.state.lookup_all(request.name)
        else:
            hosts=self.state.lookup(request.name, request.qualifier)

        logging.info(f'Lookup: {request.name} | {request.qualifier if request.qualifier else "No Qualifier"} | {hosts}')
        return pb2.LookupResponse(hosts=hosts)
        
    def Delete(self, request, context):
        try:
            self.state.delete_server(request.name, request.host)
        except Exception as e:
            context.set_code(grpc.StatusCode.NOT_FOUND)
            context.set_details(str(e))
        
        logging.info(f'Delete: {request.name} | {request.host}')
        return pb2.DeleteResponse()
    