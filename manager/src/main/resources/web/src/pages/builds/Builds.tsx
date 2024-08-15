import { Title } from '@mantine/core';
import BuildTable from '../../components/Builds';
import Page from '../../components/Page';

export function Component() {
  return (
    <Page title="Builds">
      <Title>Builds</Title>
      <BuildTable />
    </Page>
  );
}
