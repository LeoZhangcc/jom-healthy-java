require('dotenv').config();
const express = require('express');
const mysql = require('mysql2/promise');
const cors = require('cors');

const app = express();
app.use(cors());

app.all('/', (req, res) => {
  // If it's a HEAD request, we just send the status and no body
  if (req.method === 'HEAD') {
    return res.status(200).end();
  }
  // If it's a normal browser visit (GET)
  res.status(200).send('JomHealthy API is live and running!');
});

// Connect to your live Aiven database using the secure URL
const pool = mysql.createPool(process.env.DATABASE_URL);

app.get('/api/drinks/search', async (req, res) => {
  const { q } = req.query; // The English search text

  try {
    // Search the original English description column
    const [rows] = await pool.query(
      `SELECT fdc_id, description, sugar_g_per_100g 
       FROM beverages_nutrition 
       WHERE description LIKE ? 
       LIMIT 10`, 
      [`%${q}%`]
    );

    const formattedResults = rows.map(drink => {
      const descLower = drink.description.toLowerCase();
      
      let emoji = '🥤'; 
      if (descLower.includes('water')) emoji = '💧';
      else if (descLower.includes('milk')) emoji = '🥛';
      else if (descLower.includes('juice')) emoji = '🧃';
      else if (descLower.includes('tea') || descLower.includes('coffee')) emoji = '☕';

      const isUnhealthy = drink.sugar_g_per_100g > 8;

      return {
        id: drink.fdc_id,
        emoji: emoji,
        amountValue: 250, 
        type: isUnhealthy ? 'unhealthy' : 'healthy',
        title: drink.description.replace('Beverages, ', '').split(',')[0], 
        sugar: drink.sugar_g_per_100g
      };
    });

    res.json(formattedResults);
  } catch (error) {
    console.error(error);
    res.status(500).json({ error: 'Database error' });
  }
});

// Render will automatically provide a PORT, or it defaults to 3000 locally
const PORT = process.env.PORT || 3000;
app.listen(PORT, () => console.log(`Server running on port ${PORT}`));