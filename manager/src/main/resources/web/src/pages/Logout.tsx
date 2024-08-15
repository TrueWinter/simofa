import { Navigate } from 'react-router-dom';

export function Component() {
  localStorage.removeItem('token');
  return <Navigate to="/login" />;
}
