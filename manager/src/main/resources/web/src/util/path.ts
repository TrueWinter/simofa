import { useLocation, useParams } from 'react-router-dom';

export function usePathPattern() {
  const location = useLocation();
  const params = useParams();

  let path = location.pathname;
  Object.entries(params).forEach(([param, value]) => {
    path = path.replace(value, `:${param}`);
  });

  return path;
}
