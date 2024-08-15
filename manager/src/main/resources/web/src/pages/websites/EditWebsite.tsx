import { useEffect, useState } from 'react';
import { Text, Title } from '@mantine/core';
import { useActionData, useParams } from 'react-router-dom';
import { notifications } from '@mantine/notifications';
import WebsiteForm, { getBody } from '../../components/forms/WebsiteForm';
import type { Website } from '../../types/java';
import { HttpResponse, get, put } from '../../util/api';
import { scrollToTop } from '../../util/scroll';
import useRefresh from '../../util/refresh';
import FormSkeleton from '../../components/FormSkeleton';
import Page from '../../components/Page';

export function Component() {
  const params = useParams();
  const refresh = useRefresh();
  const data = useActionData() as HttpResponse;
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [website, setWebsite] = useState<Website>(null);

  useEffect(() => {
    get(`/api/websites/${params.id}`).then((b) => {
      if (b.status !== 200) {
        setError(b.body.title || 'An error occurred');
      } else {
        setWebsite(b.body);
      }

      setLoading(false);
    });
  }, []);

  useEffect(() => {
    if (data) {
      if (data.status === 200) {
        notifications.show({
          message: 'Website edited'
        });
        refresh();
      } else {
        scrollToTop();
      }
    }
  }, [data]);

  return (
    <Page title="Edit Website">
      {data && data.status !== 200 && <Text c="red">{data.body.title || 'An error occurred'}</Text>}
      {error ? <Text c="red">{error}</Text> : (
        loading ? (
          <>
            <Title>Edit Website</Title>
            <FormSkeleton />
          </>
        ) : <WebsiteForm website={website} />
      )}
    </Page>
  );
}

export async function action({ params, request }) {
  return put(`/api/websites/${params.id}`, await getBody(request));
}
