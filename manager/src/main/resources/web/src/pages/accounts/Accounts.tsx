import { useEffect, useState } from 'react';
import { Group, Table } from '@mantine/core';
import HeaderWithAddButton from '../../components/HeaderWithAddButton';
import Page from '../../components/Page';
import TableSkeleton from '../../components/TableSkeleton';
import ShortUuid from '../../components/ShortUuid';
import DeleteButton from '../../components/icons/DeleteButton';
import { get, getUserId } from '../../util/api';
import Error from '../../components/Error';
import EditButton from '../../components/icons/EditButton';
import useRefresh from '../../util/refresh';
import type { Account } from '../../types/java';

export function Component() {
  const [accounts, setAccounts] = useState<Account[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const refresh = useRefresh();

  const userId = getUserId();

  useEffect(() => {
    get<Account[]>('/api/accounts').then((resp) => {
      if (resp.success === false) {
        setError(resp.body.title || 'An error occurred');
        return;
      }

      setAccounts(resp.body);
    }).catch((err) => {
      setError(err);
    }).finally(() => {
      setLoading(false);
    });
  }, []);

  return (
    <Page title="Accounts">
      <HeaderWithAddButton title="Accounts" url="/accounts/add" />
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
              {loading ? <TableSkeleton columns={3} /> : accounts.map((e) => (
                <Table.Tr key={e.id}>
                  <Table.Td><ShortUuid uuid={e.id} /></Table.Td>
                  <Table.Td>{e.username}</Table.Td>
                  <Table.Td>
                    <Group gap="xs" justify="center">
                      <EditButton to={`/accounts/${e.id}/edit`} />
                      <DeleteButton disabled={e.id === userId}
                        label={e.id === userId ? 'You cannot delete your own account' : undefined}
                        opts={{
                          url: `/api/accounts/${e.id}`,
                          uuid: e.id,
                          type: 'account',
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
