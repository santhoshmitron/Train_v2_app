import express from 'express';
import axios from 'axios';

const router = express.Router();

router.post('/gate-status', async (req, res) => {
  try {
    const { body } = req;

    const esResponse = await axios.post(
      'http://172.105.42.194:9200/kafka-index-*/_search',
      body,
      { headers: { 'Content-Type': 'application/json' } }
    );

    res.json(esResponse.data);
  } catch (error) {
    console.error('Elasticsearch proxy error:', error.message);
    res.status(500).json({ error: error.message || 'Failed to fetch data from Elasticsearch' });
  }
});

export default router;
