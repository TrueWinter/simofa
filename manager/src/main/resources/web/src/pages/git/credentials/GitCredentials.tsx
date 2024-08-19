import { useEffect, useState } from 'react';
import { Group, Table } from '@mantine/core';
import HeaderWithAddButton from '../../../components/HeaderWithAddButton';
import Page from '../../../components/Page';
import TableSkeleton from '../../../components/TableSkeleton';
import ShortUuid from '../../../components/ShortUuid';
import DeleteButton from '../../../components/icons/DeleteButton';
import { get } from '../../../util/api';
import Error from '../../../components/Error';
import EditButton from '../../../components/icons/EditButton';
import useRefresh from '../../../util/refresh';
import type { GitCredential } from '../../../types/java';

export function Component() {
  const [credentials, setCredentials] = useState<GitCredential[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const refresh = useRefresh();

  useEffect(() => {
    get<GitCredential[]>('/api/git-credentials').then((resp) => {
      if (resp.success === false) {
        setError(resp.body.title || 'An error occurred');
        return;
      }

      setCredentials(resp.body);
    }).catch((err) => {
      setError(err);
    }).finally(() => {
      setLoading(false);
    });
  }, []);

  return (
    <Page title="Git Credentials">
      <HeaderWithAddButton title="Git Credentials" url="/git/credentials/add" />
      {error ? <Error>{error}</Error> : (
        <Table.ScrollContainer minWidth={500}>
          <Table>
            <Table.Thead>
              <Table.Tr>
                <Table.Th>ID</Table.Th>
                <Table.Th>Username</Table.Th>
                <Table.Th>Actions</Table.Th>
              </Table.Tr>
            </Table.Thead>
            <Table.Tbody>
              {loading ? <TableSkeleton columns={3} /> : credentials.map((e) => (
                <Table.Tr key={e.id}>
                  <Table.Td><ShortUuid uuid={e.id} /></Table.Td>
                  <Table.Td>{e.username}</Table.Td>
                  <Table.Td>
                    <Group gap="xs" justify="center">
                      <EditButton to={`/git/credentials/${e.id}/edit`} />
                      <DeleteButton opts={{
                        url: `/api/git-credentials/${e.id}`,
                        uuid: e.id,
                        type: 'git credential',
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
