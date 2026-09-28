import { Router, Request, Response } from 'express';
import { v4 as uuidv4 } from 'uuid';
import { supabase } from '../lib/db';

const router = Router();

// GET /api/user/:id/stats - Get user open stats
router.get('/user/:id/stats', async (req: Request, res: Response) => {
  try {
    const { id } = req.params;
    const { data, error } = await supabase
      .from('user_profiles')
      .select('*')
      .eq('id', id)
      .single();

    if (error) throw error;
    res.json({ user: data });
  } catch (err: any) {
    res.status(500).json({ error: err.message });
  }
});

// POST /api/user - Save or update user profile (name, device, IP, network)
router.post('/user', async (req: Request, res: Response) => {
  try {
    const { name, device_id, ip_address, network_info } = req.body;
    const userId = req.user?.id || uuidv4();

    const { data, error } = await supabase
      .from('user_profiles')
      .upsert({
        id: userId,
        name,
        device_id,
        ip_address,
        network_info,
        total_opens: 0,
        daily_opens: 0,
        weekly_opens: 0,
      })
      .select()
      .single();

    if (error) throw error;
    res.json({ user: data });
  } catch (err: any) {
    res.status(500).json({ error: err.message });
  }
});

// POST /api/user/:id/opens - Track app open
router.post('/user/:id/opens', async (req: Request, res: Response) => {
  try {
    const { id } = req.params;

    // Increment opens
    const { data, error } = await supabase
      .from('user_profiles')
      .update({
        total_opens: supabase.rpc('increment_total_opens', { profile_id: id }),
        daily_opens: supabase.rpc('increment_daily_opens', { profile_id: id }),
        weekly_opens: supabase.rpc('increment_weekly_opens', { profile_id: id }),
        last_open_at: new Date().toISOString(),
      })
      .eq('id', id)
      .select()
      .single();

    if (error) throw error;

    // Also update app_stats
    await supabase.from('app_stats').upsert({
      id: id,
      total_opens: supabase.rpc('increment_total_opens', { profile_id: id }),
      daily_open_count: supabase.rpc('increment_daily_opens', { profile_id: id }),
      weekly_open_count: supabase.rpc('increment_weekly_opens', { profile_id: id }),
    });

    res.json({ user: data });
  } catch (err: any) {
    res.status(500).json({ error: err.message });
  }
});

// GET /api/leaderboard - Get leaderboard ranked by opens
router.get('/leaderboard', async (req: Request, res: Response) => {
  try {
    const { data, error } = await supabase
      .from('user_profiles')
      .select('name, total_opens, device_id')
      .order('total_opens', { ascending: false })
      .limit(20);

    if (error) throw error;

    const ranked = data?.map((user: any, index: number) => ({
      rank: index + 1,
      name: user.name,
      opens: user.total_opens,
      id: user.id,
    }));

    res.json({ leaderboard: ranked });
  } catch (err: any) {
    res.status(500).json({ error: err.message });
  }
});

// GET /api/admin/stats - Get admin statistics
router.get('/admin/stats', async (req: Request, res: Response) => {
  try {
    const { data: totalUsers, error: userError } = await supabase
      .from('user_profiles')
      .select('*');

    const { data: appStats, error: statsError } = await supabase
      .from('app_stats')
      .select('*')
      .single();

    if (userError || statsError) throw userError || statsError;

    res.json({
      totalUsers: totalUsers?.length || 0,
      totalOpens: appStats?.total_opens || 0,
      dailyOpens: appStats?.daily_open_count || 0,
      weeklyOpens: appStats?.weekly_open_count || 0,
    });
  } catch (err: any) {
    res.status(500).json({ error: err.message });
  }
});

// GET /api/admin/users - Get all users
router.get('/admin/users', async (req: Request, res: Response) => {
  try {
    const { data, error } = await supabase
      .from('user_profiles')
      .select('*');

    if (error) throw error;
    res.json({ users: data });
  } catch (err: any) {
    res.status(500).json({ error: err.message });
  }
});

// POST /api/admin/block - Block a user by device/IP
router.post('/admin/block', async (req: Request, res: Response) => {
  try const { userId, deviceId, ip } = req.body;

    const { data, error } = await supabase
      .from('blocked_users')
      .upsert({
        blocked_device_id: deviceId,
        blocked_ip: ip,
        blocked_by_admin: userId,
        block_reason: 'Admin blocked',
        is_active: true,
      })
      .select()
      .single();

    if (error) throw error;
    res.json({ blocked: data });
  } catch (err: any) {
    res.status(500).json({ error: err.message });
  }
});

// GET /api/admin/blocked - Get all blocked users
router.get('/admin/blocked', async (req: Request, Response) => {
  try {
    const { data, error } = await supabase
      .from('blocked_users')
      .select('*');

    if (error) throw error;
    res.json({ blockedUsers: data });
  } catch (err: any) {
    res.status(500).json({ error: err.message });
  }
});

export default router;