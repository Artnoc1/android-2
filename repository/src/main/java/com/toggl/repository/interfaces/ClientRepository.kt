package com.toggl.repository.interfaces

import com.toggl.models.domain.Client

interface ClientRepository {
    suspend fun loadClients(): List<Client>
    suspend fun createClient(client: Client): Client
}
