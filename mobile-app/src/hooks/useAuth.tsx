import { useState, useEffect } from 'react';
import AsyncStorage from '@react-native-async-storage/async-storage';
import axios from 'axios';

const ADMIN_PASSWORD = process.env.ADMIN_PASSWORD || 'newtncsite';

export const useAuth = () => {
  const [isLoading, setIsLoading] = useState(true);
  const [user, setUser] = useState(null);
  const [isAdmin, setIsAdmin] = useState(false);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    initApp();
  }, []);

  const initApp = async () => {
    try {
      const name = await AsyncStorage.getItem('user_name');
      const deviceId = await AsyncStorage.getItem('device_id');
      const ipAddress = await AsyncStorage.getItem('ip_address');
      const networkInfo = await AsyncStorage.getItem('network_info');
      const isAdminLogged = await AsyncStorage.getItem('is_admin_logged');

      if (name) {
        setUser({ name, deviceId, ipAddress, networkInfo });
      }

      if (isAdminLogged === 'true') {
        setIsAdmin(true);
      }
      setIsLoading(false);
    } catch (err) {
      setIsLoading(false);
    }
  };

  const saveUserInfo = async (name: string, deviceId: string, ipAddress: string, networkInfo: string) => {
    try {
      await AsyncStorage.setItem('user_name', name);
      await AsyncStorage.setItem('device_id', deviceId);
      await AsyncStorage.setItem('ip_address', ipAddress);
      await AsyncStorage.setItem('network_info', networkInfo);

      const response = await axios.post(
        'http://localhost:3000/api/user',
        { name, device_id: deviceId, ip_address: ipAddress, network_info: networkInfo },
        { headers: { 'Content-Type': 'application/json' } }
      );
      setUser(response.data.user);
    } catch (err) {
      setError('Failed to save user info');
    }
  };

  const loginAsAdmin = async (password: string) => {
    if (password === ADMIN_PASSWORD) {
      try {
        await AsyncStorage.setItem('is_admin_logged', 'true');
        setIsAdmin(true);
        return { success: true };
      } catch (err) {
        setError('Failed to save admin session');
        return { success: false };
      }
    } else {
      setError('Invalid admin password');
      return { success: false, error: 'Invalid password' };
    }
  };

  const logoutAdmin = async () => {
    try {
      await AsyncStorage.setItem('is_admin_logged', 'false');
      setIsAdmin(false);
    } catch (err) {
      setError('Failed to logout admin');
    }
  };

  const trackAppOpen = async () => {
    try {
      if (!user) return;

      // Increment opens
      const today = new Date().toISOString().split('T')[0];
      const thisWeekStart = new Date();
      thisWeekStart.setDate(thisWeekStart.getDate() - new Date().getDay());
      const weekStart = thisWeekStart.toISOString().split('T')[0];

      // Update user profile opens
      await axios.patch(
        `http://localhost:3000/api/user/${user.id}/opens`,
        {},
        { headers: { 'Content-Type': 'application/json' } }
      );

      // Update app stats
      await axios.patch(
        'http://localhost:3000/api/app-stats',
        { daily_open_count: 1, weekly_open_count: 1 },
        { headers: { 'Content-Type': 'application/json' } }
      );

    } catch (err) {
      console.error('Failed to track app open:', err);
    }
  };

  return {
    isLoading,
    user,
    isAdmin,
    loginAsAdmin,
    logoutAdmin,
    saveUserInfo,
    trackAppOpen,
    error,
    setError,
  };
};