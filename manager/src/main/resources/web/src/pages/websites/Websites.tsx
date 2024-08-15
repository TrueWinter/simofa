import { useEffect, useState } from 'react';
import { Group, Table } from '@mantine/core';
import { IconTrash } from '@tabler/icons-react';
import type { Website, DeployServer } from '../../types/java';
import { get } from '../../util/api';
import ShortUuid from '../../components/ShortUuid';
import TableSkeleton from '../../components/TableSkeleton';
import useRefresh from '../../util/refresh';
import HeaderWithAddButton from '../../components/HeaderWithAddButton';
import TriggerBuild from '../../components/icons/TriggerBuildButton';
import DeleteButton from '../../components/icons/DeleteButton';
import Page from '../../components/Page';
import EditButton from '../../components/icons/EditButton';
import LogsButton from '../../components/icons/LogsButton';

export function Component() {
  const [websites, setWebsites] = useState([] as Website[]);
  const [servers, setServers] = useState(new Map() as Map<String, DeployServer>);
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(true);
  const refresh = useRefresh();

  useEffect(() => {
    Promise.all([
      get<DeployServer[]>('/api/deploy-servers'),
      get<Website[]>('/api/websites')
    ]).then((d) => {
      const deploymentServersResp = d[0];
      if (deploymentServersResp.success === false) {
        return setError(deploymentServersResp.body.title || 'An error occurred');
      }
      setServers(new Map(deploymentServersResp.body.map((v) => [v.id, v])));

      const websitesResp = d[1];
      if (websitesResp.success === false) {
        setError(websitesResp.body.title || 'An error occurred');
        return;
      }
      setWebsites(websitesResp.body);
    }).catch((e) => {
      setError(`An error occurred while fetching data: ${e}`);
    }).finally(() => {
      setLoading(false);
    });
  }, []);

  return (
    <Page title="Websites">
      <HeaderWithAddButton title="Websites" url="/websites/add" />
      {error ? <div className="error">{error}</div> : (
        <Table.ScrollContainer minWidth={500}>
          <Table>
            <Table.Thead>
              <Table.Tr>
                <Table.Th>ID</Table.Th>
                <Table.Th>Name</Table.Th>
                <Table.Th>Server</Table.Th>
                <Table.Th>Actions</Table.Th>
              </Table.Tr>
            </Table.Thead>
            <Table.Tbody>
              {loading ? <TableSkeleton columns={4} /> : websites.map((e) => (
                <Table.Tr key={e.id}>
                  <Table.Td><ShortUuid uuid={e.id} /></Table.Td>
                  <Table.Td>{e.name}</Table.Td>
                  <Table.Td>
                    <ShortUuid uuid={e.deployServer} brackets />&nbsp;
                    {servers.get(e.deployServer)?.name}
                  </Table.Td>
                  <Table.Td>
                    <Group justify="center" gap="xs" wrap="nowrap">
                      <EditButton to={`/websites/${e.id}/edit`} />
                      <LogsButton to={`/websites/${e.id}/builds`} />
                      <TriggerBuild website={e} />
                      <DeleteButton opts={{
                        url: `/api/websites/${e.id}`,
                        type: 'website',
                        uuid: e.id,
                        cb: refresh
                      }}>
                        <IconTrash />
                      </DeleteButton>
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
