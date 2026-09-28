import React, { useState, useEffect } from 'react';
import { View, Text, TextInput, TouchableOpacity, StyleSheet, Alert, FlatList, ActivityIndicator } from 'react-native';
import axios from 'axios';
import { useAuth } from '../hooks/useAuth';

export const AdminPanel = () => {
  const { user, loginAsAdmin, logoutAdmin, isAdmin } = useAuth();
  const [adminPassword, setAdminPassword] = useState('');
  const [showLogin, setShowLogin] = useState(true);
  const [users, setUsers] = useState<any[]>([]);
  const [blockedUsers, setBlockedUsers] = useState<any[]>([]);
  const [isLoading, setIsLoading] = useState(true);
  const [refreshing, setRefreshing] = useState(false);

  // Admin login logic
  const handleAdminLogin = async () => {
    const result = await loginAsAdmin(adminPassword);
    if (result.success) {
      setShowLogin(false);
    }
  };

  const handleLogout = async () => {
    await logoutAdmin();
    setShowLogin(true);
    setAdminPassword('');
  };

  // Fetch users if not admin logged in
  useEffect(() => {
    if (!isAdmin) {
      fetchUsers();
      fetchBlockedUsers();
    }
  }, [isAdmin]);

  const fetchUsers = async () => {
    try {
      const res = await axios.get('http://localhost:3000/api/admin/users');
      setUsers(res.data);
    } catch (err) {
      console.error('Failed to fetch users:', err);
    }
  };

  const fetchBlockedUsers = async () => {
    try {
      const res = await axios.get('http://localhost:3000/api/admin/blocked');
      setBlockedUsers(res.data);
    } catch (err) {
      console.error('Failed to fetch blocked users:', err);
    }
  };

  const blockUser = async (userId: string, deviceId: string, ip: string) => {
    try {
      await axios.post('http://localhost:3000/api/admin/block', {
        userId,
        deviceId,
        ip,
        reason: 'Admin blocked'
      }, {
        headers: { Authorization: `Bearer ${process.env.ADMIN_PASSWORD}` }
      });
      fetchBlockedUsers();
      fetchUsers();
      Alert.alert('Success', 'User has been blocked');
    } catch (err) {
      Alert.alert('Error', 'Failed to block user');
    }
  };

  const unblockUser = async (userId: string) => {
    try {
      await axios.delete(`http://localhost:3000/api/admin/block/${userId}`, {
        headers: { Authorization: `Bearer ${process.env.ADMIN_PASSWORD}` }
      });
      fetchBlockedUsers();
      fetchUsers();
      Alert.alert('Success', 'User has been unblocked');
    } catch (err) {
      Alert.alert('Error', 'Failed to unblock user');
    }
  };

  if (showLogin) {
    return (
      <View style={styles.loginContainer}>
        <Text style={styles.title}>Admin Login</Text>
        <Text style={styles.subtext}>Enter admin password</Text>
        <TextInput
          style={styles.input}
          placeholder="Password"
          value={adminPassword}
          onChangeText={setAdminPassword}
          secureTextEntry
        />
        <TouchableOpacity style={styles.button} onPress={handleAdminLogin}>
          <Text style={styles.buttonText}>Login</Text>
        </TouchableOpacity>
      </View>
    );
  }

  if (isLoading) {
    return <ActivityIndicator size="large" />;
  }

  return (
    <View style={styles.container}>
      {/* Stats overview */}
      <View style={styles.statsOverview}>
        <View style={style.statItem}>
          <Text style={styles.statValue}>--</Text>
          <Text style={styles.statLabel}>Total Users</Text>
        </View>
        <View style={style.statItem}>
          <Text style={styles.statValue}>--</Text>
          <Text style={styles.statLabel}>Active Today</Text>
        </View>
      </View>

      {/* Users list */}
      <View style={styles.section}>
        <Text style={styles.sectionTitle}>Users ({users.length})</Text>
        <FlatList
          data={users}
          keyExtractor={(item: any) => item.id}
          renderItem={({ item: user }: any) => renderUserItem(user)}
          style={styles.userList}
        />
      </View>

      {/* Blocked users list */}
      <View style={styles.section}>

        <Text style={styles.sectionTitle}>Blocked Users ({blockedUsers.length})</Text>
        <FlatList
          data={blockedUsers}
          keyExtractor={(item: any) => item.id}
          renderItem={({ item: block }: any) => renderBlockedItem(block)}
          style={styles.userList}
        />
      </View>
    </View>
  );
};

const renderUserItem = (user: any) => (
  <View style={styles.userItem}>
    <Text style={styles.userName}>{user.name}</Text>
    <Text style={styles.userId}>{user.device_id || 'No device'}</Text>
    <TouchableOpacity style={styles.blockBtn} onPress={() => blockUser(user.id, user.device_id, user.ip_address)}>
      <Text style={styles.blockBtnText}>Block</Text>
    </TouchableOpacity>
  </View>
);

const renderBlockedItem = (block: any) => (
  <View style={styles.userItem}>
    <Text style={styles.userName}>{block.blocked_device_id || block.blocked_ip}</Text>
    <Text style={styles.userId}>Blocked by admin</Text>
    <TouchableOpacity style={styles.blockBtn} onPress={() => unblockUser(block.id)}>
      <Text style={styles.blockBtnText}>Unblock</Text>
    </TouchableOpacity>
  </View>
);

const styles = StyleSheet.create({
  loginContainer: {
    flex: 1,
    justifyContent: 'center',
    alignItems: 'center',
    padding: 20,
    backgroundColor: '#1a1a1a',
  },
  title: {
    fontSize: 24,
    color: 'white',
    marginBottom: 20,
  },
  subtext: {
    fontSize: 14,
    color: '#aaa',
    marginBottom: 20,
  },
  input: {
    width: '100%',
    height: 50,
    backgroundColor: '#333',
    borderRadius: 8,
    paddingHorizontal: 15,
    color: 'white',
    fontSize: 16,
    marginBottom: 10,
  },
  button: {
    width: '100%',
    height: 50,
    backgroundColor: '#007AFF',
    borderRadius: 8,
    justifyContent: 'center',
    alignItems: 'center',
  },
  buttonText: {
    color: 'white',
    fontSize: 16,
    fontWeight: '600',
  },
  container: {
    flex: 1,
    backgroundColor: '#f5f5f5',
    padding: 20,
  },
  statsOverview: {
    flexDirection: 'row',
    marginBottom: 20,
  },
  statItem: {
    flex: 1,
    backgroundColor: 'white',
    padding: 15,
    borderRadius: 8,
    marginRight: 10,
  },
  statValue: {
    fontSize: 24,
    fontWeight: 'bold',
    color: '#007AFF',
  },
  statLabel: {
    fontSize: 12,
    color: '#666',
    marginTop: 3,
  },
  section: {
    backgroundColor: 'white',
    padding: 15,
    borderRadius: 8,
    marginBottom: 15,
  },
  sectionTitle: {
    fontSize: 16,
    fontWeight: 'bold',
    color: '#333',
    marginBottom: 10,
  },
  userList: {
    flex: 1,
  },
  userItem: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'center',
    padding: 10,
    marginBottom: 5,
    backgroundColor: '#f9f9f9',
    borderRadius: 6,
  },
  userName: {
    fontSize: 14,
    color: '#333',
    flex: 1,
  },
  userId: {
    fontSize: 12,
    color: '#666',
    maxWidth: 150,
    textAlign: 'right',
  },
  blockBtn: {
    backgroundColor: '#ff3b30',
    paddingHorizontal: 8,
    paddingVertical: 4,
    borderRadius: 4,
    marginLeft: 10,
  },
  blockBtnText: {
    color: 'white',
    fontSize: 12,
  },
});