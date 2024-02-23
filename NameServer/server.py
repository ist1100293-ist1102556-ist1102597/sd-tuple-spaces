import sys
sys.path.insert(1, '../Contract/target/generated-sources/protobuf/python')

from concurrent import futures
import NameServer_pb2 as pb2
import NameServer_pb2_grpc as pb2_grpc
import grpc
from naming_server_service_impl import NamingServerServiceImpl
import logging

# define the port
PORT = 5001

def main():
    # print received arguments
    print("Received arguments:")
    for i in range(1, len(sys.argv)):
        print("  " + sys.argv[i])

    root = logging.getLogger()
    root.setLevel(logging.DEBUG)

    handler = logging.StreamHandler(sys.stdout)
    handler.setLevel(logging.DEBUG)
    formatter = logging.Formatter('[%(asctime)s] [%(levelname)s] %(message)s')
    handler.setFormatter(formatter)
    root.addHandler(handler)
    
    # get port
    port = PORT

    # create a gRPC server
    server = grpc.server(futures.ThreadPoolExecutor(max_workers=1))

    # add service implementation
    pb2_grpc.add_NameServerServicer_to_server(NamingServerServiceImpl(), server)

    # listen on the port
    server.add_insecure_port('[::]:' + str(port))

    # start the server
    server.start()

    print("NameServer started on port " + str(port))
    print("Press Ctrl+C to stop...")

    server.wait_for_termination()

if __name__ == '__main__':
    try:
        exit(main())
    except KeyboardInterrupt:
        print("NameServer stopped")
        exit(0)
