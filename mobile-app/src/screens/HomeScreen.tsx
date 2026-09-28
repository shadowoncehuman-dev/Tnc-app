import React, { useEffect } from 'react';
import { View, Text, FlatList, StyleSheet, RefreshControl, ActivityIndicator } from 'react-native';
import axios from 'axios';
import { useAuth } from '../hooks/useAuth';

export const HomeScreen = ({ navigation }: any) => {
  const { user } = useAuth();
  const [opens, setOpens] = useState(0);
  const [dailyOpens, setDailyOpens] = useState(0);
  const [weeklyOpens, setWeeklyOpens] = useState(0);
  const [leaderboard, setLeaderboard] = useState<any[]>([]);
  const [refreshing, setRefreshing] = useState(false);

  useEffect(() => {
    if (user) {
      fetchUserStats();
      fetchLeaderboard();
    }
  }, [user]);

  const fetchUserStats = async () => {
    try {
      const res = await axios.get(`http://localhost:3000/api/user/${user.id}/stats`);
      setOpens(res.data.total_opens || 0);
      setDailyOpens(res.data.daily_opens || 0);
      setWeeklyOpens(res.data.weekly_opens || 0);
    } catch (err) {
      console.error('Failed to fetch stats:', err);
    }
  };

  const fetchLeaderboard = async () => {
    try {
      const res = await axios.get('http://localhost:3000/api/leaderboard');
      setLeaderboard(res.data);
    } catch (err) {
      console.error('Failed to fetch leaderboard:', err);
    }
  };

  const handleRefresh = async () => {
    setRefreshing(true);
    await fetchLeaderboard();
    setRefreshing(false);
  };

  const renderUser = ({ item }: any) => (
    <View style={styles.userRow}>
      <Text style={styles.rank}>{item.rank}.</Text>
      <Text style={styles.name}>{item.name}</Text>
      <Text style={styles.opens}>{item.opens} opens</Text>
    </View>
  );

  return (
    <View style={styles.container}>
      <Text style={styles.header}>TNC Dashboard</Text>

      {/* User stats */}
      <View style={styles.statsRow}>
        <View style={styles.statBox}>
          <Text style={styles.statValue}>{opens}</Text>
          <Text style={styles.statLabel}>Total Opens</Text>
        </View>
        <View style={styles.statBox}>
          <Text style={styles.statValue}>{dailyOpens}</Text>
          <Text style={styles.statLabel}>Today</Text>
        </View>
        <View style={styles.statBox}>
          <Text style={styles.statValue}>{weeklyOpens}</Text>
          <Text style={styles.statLabel}>This Week</Text>
        </View>
      </View>

      {/* Leaderboard */}
      <View style={styles.section}>
        <Text style={styles.sectionTitle}>Leaderboard</Text>
        <FlatList
          data={leaderboard}
          renderItem={renderUser}
          keyExtractor={(item) => item.id}
          refreshControl={
            <RefreshControl
              refreshing={refreshing}
              onRefresh={handleRefresh}
              title="Pull to refresh"
            />
          }
          style={styles.leaderboardList}
        />
        {leaderboard.length === 0 && <ActivityIndicator size="small" />}
      </View>
    </View>
  );
};

const styles = StyleSheet.create({
  container: {
    flex: 1,
    backgroundColor: '#f5f5f5',
    padding: 20,
  },
  header: {
    fontSize: 24,
    fontWeight: 'bold',
    color: '#333',
    marginBottom: 20,
  },
  statsRow: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    marginBottom: 20,
  },
  statBox: {
    backgroundColor: 'white',
    padding: 15,
    borderRadius: 8,
    width: '30%',
  },
  statValue: {
    fontSize: 20,
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
    marginBottom: 20,
  },
  sectionTitle: {
    fontSize: 18,
    fontWeight: 'bold',
    color: '#333',
    marginBottom: 15,
  },
  leaderboardList: {
    flex: 1,
  },
  userRow: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'center',
    padding: 10,
    marginBottom: 8,
    backgroundColor: '#f9f9f9',
    borderRadius: 6,
  },
  rank: {
    fontSize: 14,
    fontWeight: 'bold',
    color: '#007AFF',
    minWidth: 25,
  },
  name: {
    fontSize: 14,
    flex: 1,
    color: '#333',
  },
  opens: {
    fontSize: 14,
    color: '#666',
  },
});