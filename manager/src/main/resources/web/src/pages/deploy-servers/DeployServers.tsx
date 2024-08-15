import { useEffect, useState } from 'react';
import { Group, Table } from '@mantine/core';
import HeaderWithAddButton from '../../components/HeaderWithAddButton';
import Page from '../../components/Page';
import TableSkeleton from '../../components/TableSkeleton';
import ShortUuid from '../../components/ShortUuid';
import DeleteButton from '../../components/icons/DeleteButton';
import { get } from '../../util/api';
import Error from '../../components/Error';
import EditButton from '../../components/icons/EditButton';
import useRefresh from '../../util/refresh';
import type { DeployServer } from '../../types/java';

export function Component() {
  const [servers, setServers] = useState<DeployServer[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const refresh = useRefresh();

  useEffect(() => {
    get<DeployServer[]>('/api/deploy-servers').then((resp) => {
      if (resp.success === false) {
        setError(resp.body.title || 'An error occurred');
        return;
      }

      setServers(resp.body);
    }).catch((err) => {
      setError(err);
    }).finally(() => {
      setLoading(false);
    });
  }, []);

  return (
    <Page title="Deploy Servers">
      <HeaderWithAddButton title="Deploy Servers" url="/deploy-servers/add" />
      {error ? <Error>{error}</Error> : (
        <Table.ScrollContainer minWidth={500}>
          <Table>
            <Table.Thead>
              <Table.Tr>
                <Table.Th>ID</Table.Th>
                <Table.Th>Name</Table.Th>
                <Table.Th>Actions</Table.Th>
              </Table.Tr>
            </Table.Thead>
            <Table.Tbody>
              {loading ? <TableSkeleton columns={3} /> : servers.map((e) => (
                <Table.Tr key={e.id}>
                  <Table.Td><ShortUuid uuid={e.id} /></Table.Td>
                  <Table.Td>{e.name}</Table.Td>
                  <Table.Td>
                    <Group gap="xs" justify="center">
                      <EditButton to={`/deploy-servers/${e.id}/edit`} />
                      <DeleteButton opts={{
                        url: `/api/deploy-servers/${e.id}`,
                        uuid: e.id,
                        type: 'deploy server',
                        cb: refresh
                      }} />
                    </Group>
                  </Table.Td>
                </Table.Tr>
              ))}
            </Table.Tbody>
          </Table>
        </Table.ScrollContainer>
      )}
    </Page>
  );
}
