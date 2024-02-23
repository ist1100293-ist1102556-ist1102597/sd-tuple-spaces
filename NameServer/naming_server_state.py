class ServerEntry:
    
    def __init__(self, qualifier: str, host: str):
        self.qualifier = qualifier
        self.host = host

class ServiceEntry:

    def __init__(self, name: str):
        self.name = name
        self.servers = []

    def register_server(self, server: ServerEntry):
        self.servers.append(server)

    def delete_server(self, host: str):
        servers = filter(lambda server: server.host == host, self.servers)
        if len(list(servers)) == 0:
            raise Exception("Server not found")
        
        self.servers = list(filter(lambda server: server.host != host, self.servers))

class NamingServer:
    
    services = {}

    def register_service(self, service: ServiceEntry):
        self.services[service.name] = service

    def register_server(self, service: str, server: ServerEntry):
        if self.services.get(service) == None:
            self.register_service(ServiceEntry(service))

        self.services.get(service).register_server(server)

    def lookup_all(self, service: str):
        service_entry: ServiceEntry = self.services.get(service)
        if service_entry == None:
            return []

        return list(map(lambda server: server.host, service_entry.servers))
    
    def lookup(self, service: str, qualifier: str):
        service_entry = self.services.get(service)

        if service_entry == None:
            return []

        servers = filter(lambda server: server.qualifier == qualifier, service_entry.servers)
        return list(map(lambda server: server.host, servers))
    
    def delete_server(self, service_name: str, host: str):
        service = self.services.get(service_name)
        if service == None:
            raise Exception("Service not found")
        
        service.delete_server(host)

    def count(self):
        return sum([len(service.servers) for service in self.services.values()])