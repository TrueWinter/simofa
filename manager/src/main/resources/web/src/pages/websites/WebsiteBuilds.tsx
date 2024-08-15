import { useParams } from 'react-router-dom';
import { useEffect, useState } from 'react';
import ShortUuid from '../../components/ShortUuid';
import Builds from '../../components/Builds';
import HeaderWithButton from '../../components/HeaderWithButton';
import type { Website } from '../../types/java';
import { get } from '../../util/api';
import TriggerBuild from '../../components/icons/TriggerBuildButton';
import Page from '../../components/Page';

export function Component() {
  const { id } = useParams();
  const [website, setWebsite] = useState<Website>(null);

  useEffect(() => {
    get(`/api/websites/${id}`).then((b) => {
      if (b.status === 200) {
        setWebsite(b.body);
      }
    });
  }, []);

  return (
    <Page title="Builds">
      <HeaderWithButton title={<>Website Builds: <ShortUuid uuid={id} /></>}>
        <TriggerBuild website={website} size="lg" disabled={!website} />
      </HeaderWithButton>
      <Builds website={id} />
    </Page>
  );
}
